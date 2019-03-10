package info.maaskant.wmsnotes.model

data class Path(val elements: List<String>) {
    constructor(vararg elements: String) : this(elements.toList())

    init {
        if (!elements.isEmpty()) {
            elements
                    .firstOrNull { it.isBlank() || it.contains('/') }
                    ?.let {
                        throw IllegalArgumentException("Invalid path ($elements)")
                    }
        }
    }

    override fun toString(): String =
            elements.joinToString(separator = "/")

    fun parent(): Path =
        if (elements.isNotEmpty()) {
            Path(elements.subList(0, elements.lastIndex))
        } else {
            throw IllegalStateException("Root path does not have a parent")
        }


    companion object {
        fun from(path: String): Path =
                if (path.isEmpty()) {
                    Path()
                } else {
                    Path(path.split('/'))
                }
    }
}