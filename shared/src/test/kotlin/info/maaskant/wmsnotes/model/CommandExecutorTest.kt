package info.maaskant.wmsnotes.model

import arrow.core.Either.Companion.right
import arrow.core.Either.Right
import arrow.core.None
import arrow.core.Some
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import info.maaskant.wmsnotes.model.CommandOrigin.LOCAL
import info.maaskant.wmsnotes.model.CommandRequest.Companion.randomRequestId
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import io.mockk.*
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random

@Suppress("LocalVariableName", "RemoveRedundantBackticks")
internal abstract class CommandExecutorTest<
        AggregateType : Aggregate<AggregateType>,
        CommandType : Command,
        RequestType : CommandRequest<CommandType>,
        MapperType : CommandToEventMapper<AggregateType>
        > {
    private val commandBus: CommandBus = mockk()
    private val eventStore: EventStore = mockk()
    private val repository: AggregateRepository<AggregateType> = mockk()
    private lateinit var commandToEventMapper: MapperType

    private lateinit var executor: CommandExecutor<AggregateType, CommandType, RequestType, MapperType>

    @BeforeEach
    fun init() {
        clearMocks(
                eventStore,
                repository
        )
        commandToEventMapper = createMockedCommandToEventMapper()
        executor = createInstance(commandBus, eventStore, repository, commandToEventMapper, Schedulers.trampoline())
    }

    @Test
    fun `execute, one command, with event produced`() {
        // Given
        val aggId = getAggId1()
        val command1 = createMockedCommand()
        val origin = randomOrigin()
        val request = createCommandRequest(aggId, listOf(command1), lastRevision = null, requestId = randomRequestId(), origin = origin)
        val agg1 = getInitialAggregate(aggId)
        val (event1Before, _, event1Applied) = createEventThatChangesAggregate(agg1)
        givenAStoredAggregate(aggId, agg1)
        givenACommandCanBeMappedToAnEvent(command = command1, lastRevision = agg1.revision, event = event1Before)
        val event1Stored = givenAnEventCanBeStored(eventIn = event1Applied)

        // When
        val result = executor.execute(request)

        // Then
        assertThat(result).isEqualTo(CommandResult(
                requestId = request.requestId,
                outcome = persistentListOf(command1 to Right(Some(event1Stored))),
                origin = origin
        ))
        verifySequence { eventStore.appendEvent(event1Applied) }
    }

    @Test
    fun `execute, one command, without event produced`() {
        // Given
        val aggId = getAggId1()
        val command1 = createMockedCommand()
        val request = createCommandRequest(aggId, listOf(command1), lastRevision = null, requestId = randomRequestId(), origin = LOCAL)
        val agg1 = getInitialAggregate(aggId)
        val event1Before = createEventThatDoesNotChangeAggregate(agg1)
        givenAStoredAggregate(aggId, agg1)
        givenACommandCanBeMappedToAnEvent(command = command1, lastRevision = agg1.revision, event = event1Before)
        givenStoringAnEventFails()

        // When
        val result = executor.execute(request)

        // Then
        assertThat(result).isEqualTo(CommandResult(
                requestId = request.requestId,
                outcome = persistentListOf(command1 to Right(None)),
                origin = LOCAL
        ))
        verify { eventStore.appendEvent(any()) wasNot Called }
    }

    @Test
    fun `execute, multiple commands`() {
        // Given
        val aggId = getAggId1()
        val command1 = createMockedCommand()
        val command2 = createMockedCommand()
        val request = createCommandRequest(aggId, listOf(command1, command2), lastRevision = null, requestId = randomRequestId(), origin = LOCAL)
        val agg1 = getInitialAggregate(aggId)
        val (event1Before, agg2, event1Applied) = createEventThatChangesAggregate(agg1)
        val (event2Before, _, event2Applied) = createEventThatChangesAggregate(agg2)
        givenAStoredAggregate(aggId, agg1)
        givenACommandCanBeMappedToAnEvent(command = command1, lastRevision = agg1.revision, event = event1Before)
        givenACommandCanBeMappedToAnEvent(command = command2, lastRevision = agg2.revision, event = event2Before)
        val event1Stored = givenAnEventCanBeStored(eventIn = event1Applied)
        val event2Stored = givenAnEventCanBeStored(eventIn = event2Applied)

        // When
        val result = executor.execute(request)

        // Then
        assertThat(result).isEqualTo(CommandResult(
                requestId = request.requestId,
                outcome = persistentListOf(
                        command1 to Right(Some(event1Stored)),
                        command2 to Right(Some(event2Stored))
                ),
                origin = LOCAL
        ))
        verifySequence {
            eventStore.appendEvent(event1Applied)
            eventStore.appendEvent(event2Applied)
        }
    }

    @Test
    fun `execute, multiple commands, first one fails`() {
        // Given
        val aggId = getAggId1()
        val command1 = createMockedCommand()
        val command2 = createMockedCommand()
        val request = createCommandRequest(aggId, listOf(command1, command2), lastRevision = null, requestId = randomRequestId(), origin = LOCAL)
        val agg1 = getInitialAggregate(aggId)
        val (event1Before, agg2, event1Applied) = createEventThatChangesAggregate(agg1)
        val (event2Before, _, event2Applied) = createEventThatChangesAggregate(agg2)
        givenAStoredAggregate(aggId, agg1)
        givenACommandCanBeMappedToAnEvent(command = command1, lastRevision = agg1.revision, event = event1Before)
        givenACommandCanBeMappedToAnEvent(command = command2, lastRevision = agg2.revision, event = event2Before)
        givenStoringAnEventFails(event = event1Applied)
        givenAnEventCanBeStored(eventIn = event2Applied)

        // When
        val result = executor.execute(request)

        // Then
        assertThat(result.requestId).isEqualTo(request.requestId)
        assertThat(result.outcome).hasSize(1)
        assertThat(result.outcome.first().first).isEqualTo(command1)
        assertThat(result.outcome.first().second.isLeft()).isTrue()
        assertThat(result.origin).isEqualTo(request.origin)
        verifySequence { eventStore.appendEvent(event1Applied) }
    }

    @Test
    fun `execute, wrong lastRevision specified in request`() {
        // Given
        val aggId = getAggId1()
        val command1 = createMockedCommand()
        val request = createCommandRequest(aggId, listOf(command1), lastRevision = 15, requestId = randomRequestId(), origin = LOCAL)
        val agg1 = getInitialAggregate(aggId)
        val (event1Before, _, event1Applied) = createEventThatChangesAggregate(agg1, lastRevision = 15)
        givenAStoredAggregate(aggId, agg1)
        givenACommandCanBeMappedToAnEvent(command = command1, lastRevision = 15, event = event1Before)
        givenStoringAnEventFails(event = event1Applied) // Because revisions do not match

        // When
        val result = executor.execute(request)

        // Then
        assertThat(result.requestId).isEqualTo(request.requestId)
        assertThat(result.outcome).hasSize(1)
        assertThat(result.outcome.first().first).isEqualTo(command1)
        assertThat(result.outcome.first().second.isLeft()).isTrue()
        assertThat(result.origin).isEqualTo(request.origin)
        verify { eventStore.appendEvent(any()) wasNot Called }
    }


    @Test
    fun `canExecuteRequestType`() {
        // Given
        val aggId = getAggId1()
        val request: RequestType = createCommandRequest(aggId, emptyList(), lastRevision = null, requestId = randomRequestId(), origin = LOCAL)

        // When
        val result: RequestType? = executor.canExecuteRequest(request)

        // Then
        assertThat(result).isEqualTo(request)
    }

    @Test
    fun `canExecuteRequestType, different type`() {
        // Given
        val request: CommandRequest<*> = mockk()

        // When
        val result: RequestType? = executor.canExecuteRequest(request)

        // Then
        assertThat(result).isEqualTo(null)
    }

    private fun givenAnEventCanBeStored(eventIn: Event): Event {
        val eventOut: Event = mockk()
        every { eventStore.appendEvent(eventIn) }.returns(right(eventOut))
        return eventOut
    }

    private fun givenStoringAnEventFails() {
        every { eventStore.appendEvent(any()) }.throws(IllegalArgumentException())
    }

    private fun givenStoringAnEventFails(event: Event) {
        every { eventStore.appendEvent(event) }.throws(IllegalArgumentException())
    }

    private fun givenACommandCanBeMappedToAnEvent(command: CommandType, lastRevision: Int, event: Event) {
        every { commandToEventMapper.map(command, lastRevision = lastRevision) }.returns(event)
    }

    private fun givenAStoredAggregate(aggId: String, agg: AggregateType) {
        every { repository.getLatest(aggId) }.returns(agg)
    }

    private fun randomOrigin(): CommandOrigin {
        val values = CommandOrigin.values()
        return values[Random.nextInt(values.size)]
    }

    protected abstract fun createMockedCommandToEventMapper(): MapperType
    protected abstract fun createInstance(commandBus: CommandBus, eventStore: EventStore, repository: AggregateRepository<AggregateType>, commandToEventMapper: MapperType, scheduler: Scheduler): CommandExecutor<AggregateType, CommandType, RequestType, MapperType>
    protected abstract fun createMockedCommand(): CommandType
    protected abstract fun createCommandRequest(aggId: String, commands: List<CommandType>, lastRevision: Int?, requestId: Int, origin: CommandOrigin): RequestType
    protected abstract fun createEventThatChangesAggregate(agg: AggregateType): Triple<Event, AggregateType, Event>
    private fun createEventThatChangesAggregate(agg: AggregateType, lastRevision: Int): Triple<Event, AggregateType, Event> {
        val (eventIn, aggregate, eventOut) = createEventThatChangesAggregate(agg)
        return Triple(eventIn.copy(revision = lastRevision), aggregate, eventOut)
    }

    protected abstract fun createEventThatDoesNotChangeAggregate(agg: AggregateType): Event
    protected abstract fun getInitialAggregate(aggId: String): AggregateType
    protected abstract fun getAggId1(): String
}