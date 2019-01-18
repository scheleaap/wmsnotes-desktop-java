package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.SynchronizationStrategy.ResolutionResult.NoSolution
import info.maaskant.wmsnotes.client.synchronization.SynchronizationStrategy.ResolutionResult.Solution
import info.maaskant.wmsnotes.model.Event
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LocalOnlySynchronizationStrategyTest {
    @Test
    fun `only local events`() {
        // Given
        val event1: Event = mockk()
        val event2: Event = mockk()
        val localEvents = emptyList<Event>()
        val remoteEvents = listOf(event1, event2)
        val strategy = LocalOnlySynchronizationStrategy()

        // When
        val result = strategy.resolve(localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(Solution(listOf(
                CompensatingAction(
                        compensatedLocalEvents = emptyList(),
                        compensatedRemoteEvents = listOf(event1),
                        newLocalEvents = emptyList(),
                        newRemoteEvents = listOf(event1)
                ),
                CompensatingAction(
                        compensatedLocalEvents = emptyList(),
                        compensatedRemoteEvents = listOf(event2),
                        newLocalEvents = emptyList(),
                        newRemoteEvents = listOf(event2)
                )
        )))
    }

    @Test
    fun `local and remote events`() {
        // Given
        val event1: Event = mockk()
        val event2: Event = mockk()
        val event3: Event = mockk()
        val localEvents = listOf(event1)
        val remoteEvents = listOf(event2, event3)
        val strategy = LocalOnlySynchronizationStrategy()

        // When
        val result = strategy.resolve(localEvents = localEvents, remoteEvents = remoteEvents)

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
        val result = strategy.resolve(localEvents = localEvents, remoteEvents = remoteEvents)

        // Then
        assertThat(result).isEqualTo(NoSolution)
    }
}