package space.jetbrains.api.runtime

public expect abstract class JsonValue

internal expect fun parseJson(json: String): JsonValue?
internal expect fun JsonValue.print(): String

internal expect fun jsonNumber(number: Number): JsonValue
internal expect fun JsonValue.asNumberOrNull(): Number?
internal fun JsonValue.asNumber(link: ReferenceChainLink) = asNumberOrNull()
    ?: deserializationError("Value is expected to be a number: " + link.referenceChain())

internal expect fun jsonString(string: String): JsonValue
internal expect fun JsonValue.asStringOrNull(): String?
internal fun JsonValue.asString(link: ReferenceChainLink): String = asStringOrNull()
    ?: deserializationError("Value is expected to be a string: " + link.referenceChain())

internal expect fun jsonBoolean(boolean: Boolean): JsonValue
internal expect fun JsonValue.asBooleanOrNull(): Boolean?
internal fun JsonValue.asBoolean(link: ReferenceChainLink): Boolean = asBooleanOrNull()
    ?: deserializationError("Value is expected to be a boolean: " + link.referenceChain())

internal expect fun jsonNull(): JsonValue
internal expect fun JsonValue.isNull(): Boolean

public expect fun jsonObject(properties: Iterable<Pair<String, JsonValue>>): JsonValue
internal fun jsonObject(vararg properties: Pair<String, JsonValue>): JsonValue = jsonObject(properties.asIterable())
internal expect fun JsonValue.getField(key: String): JsonValue?
internal expect operator fun JsonValue.set(property: String, value: JsonValue)
internal expect fun JsonValue.getFieldsOrNull(): Iterable<Map.Entry<String, JsonValue>>?
internal fun JsonValue.getFields(link: ReferenceChainLink): Iterable<Map.Entry<String, JsonValue>> = getFieldsOrNull()
    ?: deserializationError("Value is expected to be an object: " + link.referenceChain())

internal expect fun jsonArray(vararg elements: JsonValue): JsonValue
internal expect fun JsonValue.arrayElementsOrNull(): Iterable<JsonValue>?
internal fun JsonValue.arrayElements(link: ReferenceChainLink): Iterable<JsonValue> = arrayElementsOrNull()
    ?: deserializationError("Value is expected to be an array: " + link.referenceChain())
