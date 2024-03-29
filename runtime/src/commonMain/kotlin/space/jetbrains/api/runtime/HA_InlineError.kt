@file:Suppress("ClassName", "PrivatePropertyName")

package space.jetbrains.api.runtime

public abstract class HA_InlineError {
    public class InaccessibleFields(
        fields: PropertyValue<List<String>>,
        message: PropertyValue<String>,
    ) : HA_InlineError() {
        private val __fields: PropertyValue<List<String>> = fields
        public val fields: List<String> get() = __fields.getValue("fields")
        private val __message: PropertyValue<String> = message
        public val message: String get() = __message.getValue("message")
    }
}

public object HA_InlineErrorInaccessibleFieldsStructure : TypeStructure<HA_InlineError.InaccessibleFields>(
    isRecord = false
) {
    private val fields: Property<List<String>> = list(string()).toProperty("fields")
    private val message: Property<String> = string().toProperty("message")

    override fun deserialize(context: DeserializationContext): HA_InlineError.InaccessibleFields {
        return HA_InlineError.InaccessibleFields(
            fields = fields.deserialize(context),
            message = message.deserialize(context),
        )
    }

    override fun serialize(value: HA_InlineError.InaccessibleFields): JsonValue = jsonObject(
        listOfNotNull(
            fields.serialize(value.fields),
            message.serialize(value.message),
        )
    )
}

public object HA_InlineErrorStructure : TypeStructure<HA_InlineError>(isRecord = false) {
    override val childClassNames: Set<String> = setOf("HA_InlineError.InaccessibleFields")

    override fun deserialize(context: DeserializationContext): HA_InlineError =
        when (val className = Type.PrimitiveType.StringType.deserialize(context.child("className"))) {
            "HA_InlineError.InaccessibleFields" -> HA_InlineErrorInaccessibleFieldsStructure.deserialize(context)
            else -> minorDeserializationError("Unsupported class name: '$className'", context.link)
        }

    override fun serialize(value: HA_InlineError): JsonValue = when (value) {
        is HA_InlineError.InaccessibleFields -> HA_InlineErrorInaccessibleFieldsStructure.serialize(value)
            .withClassName("HA_InlineError.InaccessibleFields")
        else -> error("Unsupported class ${value::class.simpleName}")
    }
}