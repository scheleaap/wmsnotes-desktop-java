package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.server.command.grpc.Command
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.utilities.persistence.StateProducer
import io.grpc.Deadline
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

data class CompensatingAction(
        val compensatedLocalEvents: List<Event>,
        val compensatedRemoteEvents: List<Event>,
        val newLocalEvents: List<Event>,
        val newRemoteEvents: List<Event>
)

class NewSynchronizer @Inject constructor(
        private val localEvents: ModifiableEventRepository,
        private val remoteEvents: ModifiableEventRepository,
        private val synchronizationStrategy: SynchronizationStrategy,
        private val compensatingActionExecutor: CompensatingActionExecutor,
        initialState: SynchronizerState?
) : StateProducer<SynchronizerState> {

    private val logger by logger()

    private var state = initialState ?: SynchronizerState.create()
    private val stateUpdates: BehaviorSubject<SynchronizerState> = BehaviorSubject.create()

    @Synchronized
    fun synchronize() {
        val localOutboundEvents = localEvents.getEvents().toList().blockingGet()
        val remoteInboundEvents = remoteEvents.getEvents().toList().blockingGet()
        val localOutboundNoteIds = localOutboundEvents
                .filter { it.eventId !in state.localEventIdsToIgnore }
                .map { it.noteId }
        val remoteInboundNoteIds = remoteInboundEvents
                .filter { it.eventId !in state.remoteEventIdsToIgnore }
                .map { it.noteId }
        updateLastRevisions(localOutboundEvents, remoteInboundEvents)
    }

    private fun updateLastRevisions(localOutboundEvents: List<Event>, remoteInboundEvents: List<Event>) {
        var stateInUpdate = state
        localOutboundEvents.forEach { event ->
            stateInUpdate = stateInUpdate.updateLastKnownLocalRevision(event.noteId, event.revision)
        }
        remoteInboundEvents.forEach { event ->
            stateInUpdate = stateInUpdate.updateLastKnownRemoteRevision(event.noteId, event.revision)
        }
        if (stateInUpdate != state) {
            updateState(stateInUpdate)
        }
    }

    private fun processLocalOutboundEvents(localOutboundEvents: Observable<Event>) {
        var connectionProblem = false
        val noteIdsWithErrors: MutableSet<String> = HashSet<String>()
        localOutboundEvents
                .filter { !connectionProblem && it.noteId !in noteIdsWithErrors }
                .forEach {
                    if (it.eventId !in state.localEventIdsToIgnore) {
                        logger.debug("Processing local event: $it")
                        val command = eventToCommandMapper.map(it, state.lastKnownRemoteRevisions[it.noteId])
                        try {
                            val request = grpcCommandMapper.toGrpcPostCommandRequest(command)
                            logger.debug("Sending command to server: $request")
                            val response = remoteCommandService
                                    .apply { if (grpcDeadline != null) this.withDeadline(grpcDeadline) }
                                    .postCommand(request)
                            if (response.status == Command.PostCommandResponse.Status.SUCCESS) {
                                localEvents.removeEvent(it)
                                updateState(state
                                        .updateLastSynchronizedLocalRevision(it.noteId, it.revision)
                                        .run {
                                            if (response.newEventId > 0) {
                                                this
                                                        .updateLastKnownRemoteRevision(it.noteId, response.newRevision)
                                                        .ignoreRemoteEvent(response.newEventId)
                                            } else {
                                                this
                                            }
                                        })
                                logger.debug("Local event successfully processed: $it")
                            } else {
                                noteIdsWithErrors += it.noteId
                                logger.debug("Command not processed by server: $request -> ${response.status} ${response.errorDescription}")
                            }
                        } catch (e: StatusRuntimeException) {
                            when (e.status.code) {
                                Status.Code.UNAVAILABLE -> {
                                    logger.debug("Could not send command for event $it to server: server not available")
                                    connectionProblem = true
                                }
                                Status.Code.DEADLINE_EXCEEDED -> {
                                    logger.debug("Could not send command for event $it to server: server is taking too long to respond")
                                    connectionProblem = true
                                }
                                else -> logger.warn("Error sending command for event $it to server: ${e.status.code}, ${e.status.description}")
                            }
                        }
                    } else {
                        localEvents.removeEvent(it)
                        updateState(state.removeLocalEventToIgnore(it.eventId))
                        logger.debug("Ignored local event $it")
                    }
                }
    }

    private fun processRemoteInboundEvents(remoteInboundEvents: Observable<Event>) {
        val noteIdsWithErrors: MutableSet<String> = HashSet<String>()
        remoteInboundEvents
                .filter { it.noteId !in noteIdsWithErrors }
                .forEach {
                    if (it.eventId !in state.remoteEventIdsToIgnore) {
                        logger.debug("Processing remote event: $it")
                        val command = eventToCommandMapper.map(it, state.lastKnownLocalRevisions[it.noteId])
                        try {
                            val localEvent = processCommandLocallyAndUpdateState(command)
                            remoteEvents.removeEvent(it)
                            if (localEvent != null) {
                                updateState(state.updateLastSynchronizedLocalRevision(it.noteId, localEvent.revision))
                            }
                            logger.debug("Remote event successfully processed: $it")
                        } catch (t: Throwable) {
                            noteIdsWithErrors += it.noteId
                            logger.debug("Command not processed locally: $command + ${state.lastKnownLocalRevisions[it.noteId]}", t)
                        }
                    } else {
                        remoteEvents.removeEvent(it)
                        updateState(state.removeRemoteEventToIgnore(it.eventId))
                        logger.debug("Ignored remote event $it")
                    }
                }
    }

    private fun processCommandLocallyAndUpdateState(command: info.maaskant.wmsnotes.model.Command): Event? {
        val event = commandProcessor.blockingProcessCommand(command)
        if (event != null) {
            updateState(state
                    .updateLastKnownLocalRevision(event.noteId, event.revision)
                    .ignoreLocalEvent(event.eventId)
            )
        }
        return event
    }

    private fun updateState(state: SynchronizerState) {
        this.state = state
        stateUpdates.onNext(state)
    }

    override fun getStateUpdates(): Observable<SynchronizerState> = stateUpdates
}