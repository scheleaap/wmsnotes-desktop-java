package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Event

class DifferenceCompensator {
    fun compensate(differences: Set<Difference>, target: Target): Set<CompensatingEvents> {
        TODO()
    }

    enum class Target {
        LEFT,
        RIGHT
    }

    data class CompensatingEvents(val leftEvents: List<Event>, val rightEvents: List<Event>)
}
