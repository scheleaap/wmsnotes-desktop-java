package info.maaskant.wmsnotes.model.eventstore

import arrow.core.Either
import info.maaskant.wmsnotes.model.CommandError
import info.maaskant.wmsnotes.model.Event
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DelayingEventStore @Inject constructor(private val wrapped: EventStore) : EventStore {
    override fun getEvents(afterEventId: Int?): Observable<Event> {
        return wrapped
                .getEvents(afterEventId)
                .delaySubscription(2, TimeUnit.SECONDS)
    }

    override fun getEventsOfAggregate(aggId: String, afterRevision: Int?): Observable<Event> {
        return wrapped
                .getEventsOfAggregate(aggId, afterRevision)
                .concatMap { Observable.just(it).delay(500, TimeUnit.MILLISECONDS) }
    }

    override fun appendEvent(event: Event): Either<CommandError.StorageError, Event> {
        return wrapped.appendEvent(event)
    }

    override fun getEventUpdates(): Observable<Event> {
        return wrapped.getEventUpdates()
    }
}
