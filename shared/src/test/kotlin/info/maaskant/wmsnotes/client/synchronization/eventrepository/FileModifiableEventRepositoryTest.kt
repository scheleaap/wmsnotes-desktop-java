package info.maaskant.wmsnotes.client.synchronization.eventrepository

import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.testutilities.FileAssertions.doesNotExist
import info.maaskant.wmsnotes.utilities.serialization.Serializer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

internal class FileModifiableEventRepositoryTest : ModifiableEventRepositoryTest() {

    private val eventSerializer: Serializer<Event> = mockk()

    private lateinit var tempDir: File

    @BeforeEach
    fun init() {
        clearMocks(eventSerializer)
        events.forEach {
            every { eventSerializer.serialize(it.first) }.returns(it.second.toByteArray())
            every { eventSerializer.deserialize(it.second.toByteArray()) }.returns(it.first)
        }

        tempDir = createTempDir(this::class.simpleName!!)
    }

    @Test
    fun `check that directory is empty on initialization`() {
        // Given
        val tempDir = createTempDir(this::class.simpleName!!)
        FileModifiableEventRepository(tempDir, eventSerializer)

        // Then
        assertThat(tempDir.list()).isEmpty()
    }


    @Test
    fun `addEvent, check file`() {
        // Given
        val r = createInstance()

        // When
        r.addEvent(events[0].first)

        // Then
        val expectedEventFile = tempDir.resolve("0000000001")
        assertThat(expectedEventFile).exists()
        assertThat(expectedEventFile.readBytes()).isEqualTo("DATA1".toByteArray())
    }


    @Test
    fun `removeEvent, check file`() {
        // Given
        val r = createInstance()
        r.addEvent(events[0].first)

        // When
        r.removeEvent(events[0].first)

        // Then
        val expectedEventFile = tempDir.resolve("0000000001")
        assertThat(expectedEventFile).doesNotExist()
    }

    override fun createInstance(): ModifiableEventRepository {
        return FileModifiableEventRepository(tempDir, eventSerializer)
    }

}