package space.jetbrains.api.runtime

public data class ReferenceChainLink(val name: String, val actualType: String? = null, val parent: ReferenceChainLink? = null) {

    public fun referenceChain(): String = buildString { referenceChain(this) }

    private fun referenceChain(sb: StringBuilder): StringBuilder {
        parent?.referenceChain(sb)?.append("->")
        return sb.append(name).also {
            if (actualType != null) {
                sb.append(":").append(actualType)
            }
        }
    }
}

public fun ReferenceChainLink.child(name: String): ReferenceChainLink = ReferenceChainLink(name, parent = this)

public fun ReferenceChainLink.withActualType(type: String): ReferenceChainLink = ReferenceChainLink(name, type, parent)

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

    public fun className(): String {
        val childContext = child("className")
        return childContext.json?.let { Type.PrimitiveType.StringType.deserialize(childContext) }
            ?: inaccessibleFieldErrorMessagesByFieldName["className"]?.let { deserializationError("$it ${link.referenceChain()}") }
            ?: deserializationError("Missing required property: ${childContext.link.referenceChain()}")
    }

    public fun withActualType(actualType: String): DeserializationContext =
        DeserializationContext(json, link.withActualType(actualType), partial)

    public fun elements(): Iterable<DeserializationContext> = sequence {
        for ((index, element) in requireJson().arrayElements(link).withIndex()) {
            yield(DeserializationContext(element, link.child("[$index]"), partial))
        }
    }.asIterable()

    internal val inaccessibleFieldErrorMessagesByFieldName by lazy {
        Type.Nullable(Type.ArrayType(Type.ObjectType(HA_InlineErrorStructure)))
            .deserialize(child("\$errors")).orEmpty().asSequence()
            .filterIsInstance<HA_InlineError.InaccessibleFields>()
            .flatMap { error ->
                error.fields.asSequence().map { it to error.message }
            }
            .toMap()
    }
}
