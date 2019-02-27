package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.CompensatingAction
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.Solution
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MergingSynchronizationStrategyTest {
    private val noteProjector: NoteProjector = mockk()
    private val mergeStrategy: MergeStrategy = mockk()

    @BeforeEach
    fun init() {
        clearMocks(
                noteProjector,
                mergeStrategy
        )
    }

    @Test
    fun solution() {
        // Given
        val baseRevision = 1
        val compensatedLocalEvent1: Event = modelEvent(eventId = 1, noteId = 1, revision = baseRevision + 1)
        val compensatedLocalEvent2: Event = modelEvent(eventId = 2, noteId = 1, revision = baseRevision + 2)
        val compensatedRemoteEvent1: Event = modelEvent(eventId = 11, noteId = 1, revision = 11)
        val compensatedRemoteEvent2: Event = modelEvent(eventId = 12, noteId = 1, revision = 12)
        val modifiedCompensatedRemoteEvent1: Event = modelEvent(eventId = 11, noteId = 1, revision = baseRevision + 1)
        val modifiedCompensatedRemoteEvent2: Event = modelEvent(eventId = 12, noteId = 1, revision = baseRevision + 2)
        val compensatedLocalEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2)
        val compensatedRemoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2)
        val modifiedCompensatedRemoteEvents = listOf(modifiedCompensatedRemoteEvent1, modifiedCompensatedRemoteEvent2)
        val newLocalEvent1: Event = mockk()
        val newLocalEvent2: Event = mockk()
        val newRemoteEvent1: Event = mockk()
        val newRemoteEvent2: Event = mockk()
        val noteId = compensatedLocalEvent1.noteId
        val baseNote: Note = mockk()
        val localNote: Note = mockk()
        val remoteNote: Note = mockk()
        every { noteProjector.project(noteId, baseRevision) }.returns(baseNote)
        givenAnEventApplicationResult(baseNote, compensatedLocalEvents, localNote)
        givenAnEventApplicationResult(baseNote, modifiedCompensatedRemoteEvents, remoteNote)
        every { mergeStrategy.merge(compensatedLocalEvents, compensatedRemoteEvents, baseNote, localNote, remoteNote) }.returns(Solution(
                newLocalEvents = listOf(newLocalEvent1, newLocalEvent2),
                newRemoteEvents = listOf(newRemoteEvent1, newRemoteEvent2)
        ))
        val strategy = MergingSynchronizationStrategy(mergeStrategy, noteProjector)

        // When
        val result = strategy.resolve(noteId = noteId, localEvents = compensatedLocalEvents, remoteEvents = compensatedRemoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.Solution(listOf(CompensatingAction(
                compensatedLocalEvents = compensatedLocalEvents,
                compensatedRemoteEvents = compensatedRemoteEvents,
                newLocalEvents = listOf(newLocalEvent1, newLocalEvent2),
                newRemoteEvents = listOf(newRemoteEvent1, newRemoteEvent2)
        ))))
    }

    @Test
    fun `no solution`() {
        // Given
        val baseRevision = 1
        val compensatedLocalEvent1: Event = modelEvent(eventId = 1, noteId = 1, revision = baseRevision+1)
        val compensatedLocalEvent2: Event = modelEvent(eventId = 2, noteId = 1, revision = baseRevision+2)
        val compensatedRemoteEvent1: Event = modelEvent(eventId = 11, noteId = 1, revision = 11)
        val compensatedRemoteEvent2: Event = modelEvent(eventId = 12, noteId = 1, revision = 12)
        val modifiedCompensatedRemoteEvent1: Event = modelEvent(eventId = 11, noteId = 1, revision = baseRevision + 1)
        val modifiedCompensatedRemoteEvent2: Event = modelEvent(eventId = 12, noteId = 1, revision = baseRevision + 2)
        val compensatedLocalEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2)
        val compensatedRemoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2)
        val modifiedCompensatedRemoteEvents = listOf(modifiedCompensatedRemoteEvent1, modifiedCompensatedRemoteEvent2)
        val noteId = compensatedLocalEvent1.noteId
        val baseNote: Note = mockk()
        val localNote: Note = mockk()
        val remoteNote: Note = mockk()
        every { noteProjector.project(noteId, baseRevision) }.returns(baseNote)
        givenAnEventApplicationResult(baseNote, compensatedLocalEvents, localNote)
        givenAnEventApplicationResult(baseNote, modifiedCompensatedRemoteEvents, remoteNote)
        every { mergeStrategy.merge(compensatedLocalEvents, compensatedRemoteEvents, baseNote, localNote, remoteNote) }.returns(NoSolution)
        val strategy = MergingSynchronizationStrategy(mergeStrategy, noteProjector)

        // When
        val result = strategy.resolve(noteId = noteId, localEvents = compensatedLocalEvents, remoteEvents = compensatedRemoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.NoSolution)
    }

    @Test
    fun `no local events`() {
        // Given
        val baseRevision = 1
        val compensatedRemoteEvent1: Event = modelEvent(eventId = 11, noteId = 1, revision = 11)
        val compensatedRemoteEvent2: Event = modelEvent(eventId = 12, noteId = 1, revision = 12)
        val modifiedCompensatedRemoteEvent1: Event = modelEvent(eventId = 11, noteId = 1, revision = baseRevision + 1)
        val modifiedCompensatedRemoteEvent2: Event = modelEvent(eventId = 12, noteId = 1, revision = baseRevision + 2)
        val compensatedLocalEvents = emptyList<Event>()
        val compensatedRemoteEvents = listOf(compensatedRemoteEvent1, compensatedRemoteEvent2)
        val modifiedCompensatedRemoteEvents = listOf(modifiedCompensatedRemoteEvent1, modifiedCompensatedRemoteEvent2)
        val newLocalEvent1: Event = mockk()
        val newLocalEvent2: Event = mockk()
        val newRemoteEvent1: Event = mockk()
        val newRemoteEvent2: Event = mockk()
        val noteId = compensatedRemoteEvent1.noteId
        val baseNote: Note = mockk()
        val localNote: Note = mockk()
        val remoteNote: Note = mockk()
        givenAnEventApplicationResult(baseNote, modifiedCompensatedRemoteEvents, remoteNote)
        every { mergeStrategy.merge(compensatedLocalEvents, compensatedRemoteEvents, baseNote, localNote, remoteNote) }.returns(Solution(
                newLocalEvents = listOf(newLocalEvent1, newLocalEvent2),
                newRemoteEvents = listOf(newRemoteEvent1, newRemoteEvent2)
        ))
        val strategy = MergingSynchronizationStrategy(mergeStrategy, noteProjector)

        // When
        val result = strategy.resolve(noteId = noteId, localEvents = compensatedLocalEvents, remoteEvents = compensatedRemoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.NoSolution)
    }

    @Test
    fun `no remote events`() {
        // Given
        val baseRevision = 1
        val compensatedLocalEvent1: Event = modelEvent(eventId = 1, noteId = 1, revision = baseRevision + 1)
        val compensatedLocalEvent2: Event = modelEvent(eventId = 2, noteId = 1, revision = baseRevision + 2)
        val compensatedLocalEvents = listOf(compensatedLocalEvent1, compensatedLocalEvent2)
        val compensatedRemoteEvents = emptyList<Event>()
        val newLocalEvent1: Event = mockk()
        val newLocalEvent2: Event = mockk()
        val newRemoteEvent1: Event = mockk()
        val newRemoteEvent2: Event = mockk()
        val noteId = compensatedLocalEvent1.noteId
        val baseNote: Note = mockk()
        val localNote: Note = mockk()
        val remoteNote: Note = mockk()
        every { noteProjector.project(noteId, baseRevision) }.returns(baseNote)
        givenAnEventApplicationResult(baseNote, compensatedLocalEvents, localNote)
        every { mergeStrategy.merge(compensatedLocalEvents, compensatedRemoteEvents, baseNote, localNote, remoteNote) }.returns(Solution(
                newLocalEvents = listOf(newLocalEvent1, newLocalEvent2),
                newRemoteEvents = listOf(newRemoteEvent1, newRemoteEvent2)
        ))
        val strategy = MergingSynchronizationStrategy(mergeStrategy, noteProjector)

        // When
        val result = strategy.resolve(noteId = noteId, localEvents = compensatedLocalEvents, remoteEvents = compensatedRemoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.NoSolution)
    }

    @Test
    fun `local event with revision 1`() {
        // This situation should not normally occur, because it would mean there is a conflict involving a local note
        // that has just been created. Perhaps it could occur in case the client crashes during a previous
        // synchronization, though.

        // Given
        val baseRevision = 0 // 0 instead of 1!
        val compensatedLocalEvent1: Event = modelEvent(eventId = 1, noteId = 1, revision = baseRevision + 1)
        val compensatedRemoteEvent1: Event = modelEvent(eventId = 11, noteId = 1, revision = 11)
        val modifiedCompensatedRemoteEvent1: Event = modelEvent(eventId = 11, noteId = 1, revision = baseRevision + 1)
        val compensatedLocalEvents = listOf(compensatedLocalEvent1)
        val compensatedRemoteEvents = listOf(compensatedRemoteEvent1)
        val modifiedCompensatedRemoteEvents = listOf(modifiedCompensatedRemoteEvent1)
        val newLocalEvent1: Event = mockk()
        val newRemoteEvent1: Event = mockk()
        val noteId = compensatedLocalEvent1.noteId
        val baseNote: Note = mockk()
        val localNote: Note = mockk()
        val remoteNote: Note = mockk()
        every { noteProjector.project(noteId, baseRevision) }.returns(baseNote)
        givenAnEventApplicationResult(baseNote, compensatedLocalEvents, localNote)
        givenAnEventApplicationResult(baseNote, modifiedCompensatedRemoteEvents, remoteNote)
        every { mergeStrategy.merge(compensatedLocalEvents, compensatedRemoteEvents, baseNote, localNote, remoteNote) }.returns(Solution(
                newLocalEvents = listOf(newLocalEvent1),
                newRemoteEvents = listOf(newRemoteEvent1)
        ))
        val strategy = MergingSynchronizationStrategy(mergeStrategy, noteProjector)

        // When
        val result = strategy.resolve(noteId = noteId, localEvents = compensatedLocalEvents, remoteEvents = compensatedRemoteEvents)

        // Then
        assertThat(result).isEqualTo(SynchronizationStrategy.ResolutionResult.Solution(listOf(CompensatingAction(
                compensatedLocalEvents = listOf(compensatedLocalEvent1),
                compensatedRemoteEvents = listOf(compensatedRemoteEvent1),
                newLocalEvents = listOf(newLocalEvent1),
                newRemoteEvents = listOf(newRemoteEvent1)
        ))))
    }

    private fun givenAnEventApplicationResult(
            baseNote: Note,
            events: List<Event>,
            resultNote: Note) {
        val last = events.last()
        val firsts = events - last
        var baseNoteTemp = baseNote
        for (first in firsts) {
            val resultNoteTemp: Note = mockk()
            every { baseNoteTemp.apply(first) }.returns(Pair(resultNoteTemp, first))
            baseNoteTemp = resultNoteTemp
        }
        every { baseNoteTemp.apply(last) }.returns(Pair(resultNote, last))
    }

    companion object {
        internal fun modelEvent(eventId: Int, noteId: Int, revision: Int): NoteCreatedEvent {
            return NoteCreatedEvent(eventId = eventId, noteId = "note-$noteId", revision = revision, title = "Title $noteId")
        }
    }
}