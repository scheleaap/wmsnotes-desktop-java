package info.maaskant.wmsnotes.model.folder

import info.maaskant.wmsnotes.model.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class FolderCommandToEventMapperTest {
    private val path = Path("el1", "el2")

    @TestFactory
    fun test(): List<DynamicTest> {
        val lastRevision = 11
        val eventRevision = lastRevision + 1

        val pairs = listOf(
                CreateFolderCommand(path = path, lastRevision = lastRevision) to FolderCreatedEvent(eventId = 0, revision = eventRevision, path = path),
                DeleteFolderCommand(path = path, lastRevision = lastRevision) to FolderDeletedEvent(eventId = 0, revision = eventRevision, path = path)
                // Add more classes here
        )
        return pairs.map { (command, expectedEvent) ->
            DynamicTest.dynamicTest("${command::class.simpleName} to ${expectedEvent::class.simpleName}") {
                assertThat(FolderCommandToEventMapper().map(command)).isEqualTo(expectedEvent)
            }
        }
    }
}