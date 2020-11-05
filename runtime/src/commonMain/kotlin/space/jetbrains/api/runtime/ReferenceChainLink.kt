package space.jetbrains.api.runtime

public data class ReferenceChainLink(val name: String, val parent: ReferenceChainLink? = null) {

    public fun referenceChain(): String = buildString { referenceChain(this) }

    private fun referenceChain(sb: StringBuilder): StringBuilder {
        parent?.referenceChain(sb)?.append("->")
        return sb.append(name)
    }
}

public fun ReferenceChainLink.child(name: String): ReferenceChainLink = ReferenceChainLink(name, this)

public sealed class DeserializationException(override val message: String) : RuntimeException(message) {
    public class Major internal constructor(message: String) : DeserializationException(message)
    public class Minor internal constructor(
        message: String,
        public val link: ReferenceChainLink
    ) : DeserializationException(message) {
        public fun major(): Major = Major(message)
    }
}

internal fun deserializationError(message: String): Nothing = throw DeserializationException.Major(message)

public data class DeserializationContext(
    val json: JsonValue?,
    val link: ReferenceChainLink,
    val partial: PartialBuilder?,
) {
    public fun requireJson(): JsonValue = json
        ?: deserializationError("Missing required property: ${link.referenceChain()}")

    public fun child(
        name: String,
        json: JsonValue? = requireJson().getField(name),
        link: ReferenceChainLink = this.link.child(name),
        partial: PartialBuilder? = this.partial?.children?.get(name)
    ): DeserializationContext = DeserializationContext(json, link, partial)

    public fun elements(): Iterable<DeserializationContext> = sequence {
        for ((index, element) in requireJson().arrayElements(link).withIndex()) {
            yield(DeserializationContext(element, link.child("[$index]"), partial))
        }
    }.asIterable()
}
