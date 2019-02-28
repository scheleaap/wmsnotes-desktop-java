package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class EventToCommandMapperTest {
    @TestFactory
    fun test(): List<DynamicTest> {
        val aggId = "note"
        val lastRevision = 11
        val eventRevision = lastRevision + 1

        val pairs = listOf(
                NoteCreatedEvent(eventId = 1, aggId = aggId, revision = eventRevision, path = Path("el1", "el2"), title = "Title", content = "Text") to CreateNoteCommand(aggId = aggId, path = Path("el1", "el2"), title = "Title", content = "Text"),
                NoteDeletedEvent(eventId = 1, aggId = aggId, revision = eventRevision) to DeleteNoteCommand(aggId = aggId, lastRevision = lastRevision),
                NoteUndeletedEvent(eventId = 1, aggId = aggId, revision = eventRevision) to UndeleteNoteCommand(aggId = aggId, lastRevision = lastRevision),
                AttachmentAddedEvent(eventId = 0, aggId = aggId, revision = eventRevision, name = "att", content = "data".toByteArray()) to AddAttachmentCommand(aggId = aggId, lastRevision = lastRevision, name = "att", content = "data".toByteArray()),
                AttachmentDeletedEvent(eventId = 0, aggId = aggId, revision = eventRevision, name = "att") to DeleteAttachmentCommand(aggId = aggId, lastRevision = lastRevision, name = "att"),
                ContentChangedEvent(eventId = 0, aggId = aggId, revision = eventRevision, content = "Text") to ChangeContentCommand(aggId = aggId, lastRevision = lastRevision, content = "Text"),
                TitleChangedEvent(eventId = 0, aggId = aggId, revision = eventRevision, title = "Title") to ChangeTitleCommand(aggId = aggId, lastRevision = lastRevision, title = "Title"),
                MovedEvent(eventId = 0, aggId = aggId, revision = eventRevision, path = Path("el1", "el2")) to MoveCommand(aggId = aggId, lastRevision = lastRevision, path = Path("el1", "el2"))
                // Add more classes here
        )
        return pairs.map { (event, expectedCommand) ->
            DynamicTest.dynamicTest("${event::class.simpleName} to ${expectedCommand::class.simpleName}") {
                assertThat(EventToCommandMapper().map(event, lastRevision = lastRevision))
                        .isEqualTo(expectedCommand)
            }
        }
    }
}