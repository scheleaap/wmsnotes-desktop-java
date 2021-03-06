package info.maaskant.wmsnotes.client.synchronization.strategy

import assertk.assertThat
import assertk.assertions.isEqualTo
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy.ResolutionResult.Solution
import info.maaskant.wmsnotes.model.Event
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class LocalOnlySynchronizationStrategyTest {
    private val aggId = "note"

    @Test
    fun `only local events`() {
        // Given
        val event1: Event = mockk()
        val event2: Event = mockk()
        val localEvents = listOf(event1, event2)
        val remoteEvents = emptyList<Event>()
        val strategy = LocalOnlySynchronizationStrategy()

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(Solution(
                compensatedLocalEvents = localEvents,
                compensatedRemoteEvents = emptyList(),
                newLocalEvents = emptyList(),
                newRemoteEvents = localEvents
        ))
    }

    @Test
    fun `local and remote events`() {
        // Given
        val event1: Event = mockk()
        val event2: Event = mockk()
        val event3: Event = mockk()
        val localEvents = listOf(event1, event2)
        val remoteEvents = listOf(event3)
        val strategy = LocalOnlySynchronizationStrategy()

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(NoSolution)
    }

    @Test
    fun `no events`() {
        // Given
        val localEvents = emptyList<Event>()
        val remoteEvents = emptyList<Event>()
        val strategy = LocalOnlySynchronizationStrategy()

        // When
        val result = strategy.resolve(aggId = aggId, localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(Solution(
                compensatedLocalEvents = emptyList(),
                compensatedRemoteEvents = emptyList(),
                newLocalEvents = emptyList(),
                newRemoteEvents = emptyList()
        ))
    }
}