package space.jetbrains.api.runtime

data class ReferenceChainLink(val name: String, val parent: ReferenceChainLink? = null) {

    fun referenceChain(): String = buildString { referenceChain(this) }

    private fun referenceChain(sb: StringBuilder): StringBuilder {
        parent?.referenceChain(sb)?.append("->")
        return sb.append(name)
    }
}

fun ReferenceChainLink.child(name: String): ReferenceChainLink = ReferenceChainLink(name, this)

data class DeserializationContext<T : Any>(
    val json: JsonValue?,
    val partial: Partial<T>?,
    val link: ReferenceChainLink
) {
    fun requireJson() = json ?: error("Missing required property: ${link.referenceChain()}")

    fun child(
        name: String,
        json: JsonValue? = requireJson()[name],
        partial: Partial<*>? = this.partial?.requestedProperties?.get(name)?.partial,
        link: ReferenceChainLink = this.link.child(name)
    ): DeserializationContext<*> = DeserializationContext(json, partial, link)

    fun elements(): Iterable<DeserializationContext<T>> = sequence {
        for ((index, element) in requireJson().arrayElements(link).withIndex()) {
            yield(DeserializationContext(element, partial, link.child("[$index]")))
        }
    }.asIterable()
}
