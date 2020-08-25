package space.jetbrains.api.runtime

public data class ReferenceChainLink(val name: String, val parent: ReferenceChainLink? = null) {

    public fun referenceChain(): String = buildString { referenceChain(this) }

    private fun referenceChain(sb: StringBuilder): StringBuilder {
        parent?.referenceChain(sb)?.append("->")
        return sb.append(name)
    }
}

public fun ReferenceChainLink.child(name: String): ReferenceChainLink = ReferenceChainLink(name, this)

public data class DeserializationContext(
    val json: JsonValue?,
    val link: ReferenceChainLink
) {
    public fun requireJson(): JsonValue = json ?: error("Missing required property: ${link.referenceChain()}")

    public fun child(
        name: String,
        json: JsonValue? = requireJson().getField(name),
        link: ReferenceChainLink = this.link.child(name)
    ): DeserializationContext = DeserializationContext(json, link)

    public fun elements(): Iterable<DeserializationContext> = sequence {
        for ((index, element) in requireJson().arrayElements(link).withIndex()) {
            yield(DeserializationContext(element, link.child("[$index]")))
        }
    }.asIterable()
}
