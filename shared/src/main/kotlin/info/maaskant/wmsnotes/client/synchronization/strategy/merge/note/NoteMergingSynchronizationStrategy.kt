package info.maaskant.wmsnotes.client.synchronization.strategy.merge.note

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergingSynchronizationStrategy
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.model.note.NoteEvent

class NoteMergingSynchronizationStrategy(
        mergeStrategy: MergeStrategy<Note>,
        aggregateRepository: AggregateRepository<Note>
) : MergingSynchronizationStrategy<Note>(mergeStrategy, aggregateRepository) {
    override fun canHandleEvent(it: Event): Boolean = it is NoteEvent
}