package info.maaskant.wmsnotes.model

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest

internal class KryoEventSerializerTest : KryoSerializerTest<Event>() {
    private val aggId = "note-1"

    override val items: List<Event> = listOf(
            NoteCreatedEvent(eventId = 1, aggId = aggId, revision = 1, path = Path("path"), title = "Title", content = "Text"),
            NoteDeletedEvent(eventId = 1, aggId = aggId, revision = 1),
            NoteUndeletedEvent(eventId = 1, aggId = aggId, revision = 1),
            AttachmentAddedEvent(eventId = 1, aggId = aggId, revision = 1, name = "att", content = "data".toByteArray()),
            AttachmentDeletedEvent(eventId = 1, aggId = aggId, revision = 1, name = "att"),
            ContentChangedEvent(eventId = 0, aggId = aggId, revision = 1, content = "Text"),
            TitleChangedEvent(eventId = 0, aggId = aggId, revision = 1, title = "Title"),
            MovedEvent(eventId = 0, aggId = aggId, revision = 1, path = Path("path"))
            // Add more classes here
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoEventSerializer(kryoPool)
}
