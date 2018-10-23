package info.maaskant.wmsnotes.model.projection.cache

import info.maaskant.wmsnotes.model.AttachmentAddedEvent
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.projection.Note
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

internal abstract class NoteCacheTest {
    protected val noteId = "note"

    protected val noteAfterEvent1 = Note().apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = "Title")).component1()
    protected val noteAfterEvent2 = noteAfterEvent1.apply(AttachmentAddedEvent(eventId = 2, noteId = noteId, revision = 2, name = "att-1", content = "data1".toByteArray())).component1()
    protected val noteAfterEvent3 = noteAfterEvent2.apply(AttachmentAddedEvent(eventId = 3, noteId = noteId, revision = 3, name = "att-2", content = "data2".toByteArray())).component1()

    private lateinit var tempDir: File

    open fun init() {
        givenANote(noteAfterEvent1)
        givenANote(noteAfterEvent2)
        givenANote(noteAfterEvent3)
    }

    @Test
    fun `put and get`() {
        // Given
        val noteIn: Note = noteAfterEvent2
        val c = createInstance()

        // When
        c.put(noteIn)
        val noteOut = c.get(noteIn.noteId, noteIn.revision)

        // Then
        assertThat(noteOut).isEqualTo(noteIn)
    }

    @Test
    fun `get, different revision`() {
        // Given
        val noteIn: Note = noteAfterEvent2

        val c = createInstance()
        c.put(noteIn)

        // When
        val noteOut = c.get(noteId, noteIn.revision + 1)

        // Then
        assertThat(noteOut).isNull()
    }

    @Test
    fun `get, nonexistent`() {
        // Given
        val noteIn: Note = noteAfterEvent2
        val c = createInstance()
        c.put(noteIn)

        // When
        val noteOut = c.get("other", noteIn.revision)

        // Then
        assertThat(noteOut).isNull()
    }

    @Test
    fun `get latest`() {
        // Given
        val c = createInstance()
        c.put(noteAfterEvent1)
        c.put(noteAfterEvent3)
        val otherNoteWithHigherRevision = givenANote(
                Note()
                        .apply(NoteCreatedEvent(eventId = 1, noteId = "other", revision = 1, title = "Title")).component1()
                        .apply(AttachmentAddedEvent(eventId = 2, noteId = "other", revision = 2, name = "att-1", content = "data1".toByteArray())).component1()
                        .apply(AttachmentAddedEvent(eventId = 3, noteId = "other", revision = 3, name = "att-2", content = "data2".toByteArray())).component1()
                        .apply(AttachmentAddedEvent(eventId = 4, noteId = "other", revision = 4, name = "att-3", content = "data3".toByteArray())).component1()
        )
        c.put(otherNoteWithHigherRevision)

        // When
        val noteOut = c.getLatest(noteId, lastRevision = null)

        // Then
        assertThat(noteOut).isEqualTo(noteAfterEvent3)
    }

    @Test
    fun `get latest, nonexistent`() {
        // Given
        val c = createInstance()

        // When
        val noteOut = c.getLatest(noteId, lastRevision = 1)

        // Then
        assertThat(noteOut).isNull()
    }

    @Test
    fun `get latest, limit revision`() {
        // Given
        val c = createInstance()
        c.put(noteAfterEvent1)
        c.put(noteAfterEvent3)

        // When
        val noteOut = c.getLatest(noteId, lastRevision = noteAfterEvent2.revision)

        // Then
        assertThat(noteOut).isEqualTo(noteAfterEvent1)
    }

    @Test
    fun `remove and get`() {
        // Given
        val c = createInstance()
        c.put(noteAfterEvent1)
        c.put(noteAfterEvent2)

        // When
        c.remove(noteId, noteAfterEvent1.revision)
        val noteOut = c.get(noteId, noteAfterEvent1.revision)

        // Then
        assertThat(noteOut).isNull()
    }

    protected abstract fun createInstance(): NoteCache

    protected abstract fun givenANote(note: Note): Note

}