package space.jetbrains.api.runtime

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.node.*
import com.fasterxml.jackson.databind.node.JsonNodeFactory.instance as jsonNodes

private val jackson = ObjectMapper()

public actual fun parseJson(json: String): JsonValue? = jackson.readTree(json)
public actual fun JsonValue.print(): String = jackson.writeValueAsString(this)

public actual typealias JsonValue = JsonNode

internal actual fun JsonValue.asNumberOrNull(): Number? = if (isNumber) numberValue() else null

internal actual fun jsonNumber(number: Number): JsonValue = when (number) {
    is Byte -> jsonNodes.numberNode(number)
    is Short -> jsonNodes.numberNode(number)
    is Int -> jsonNodes.numberNode(number)
    is Long -> jsonNodes.numberNode(number)
    is Float -> jsonNodes.numberNode(number)
    is Double -> jsonNodes.numberNode(number)
    else -> throw IllegalArgumentException("Unsupported number type: ${number.javaClass.name}")
}

internal actual fun JsonValue.asStringOrNull(): String? = if (isTextual) asText() else null

internal actual fun jsonString(string: String): JsonValue = jsonNodes.textNode(string)

internal actual fun JsonValue.asBooleanOrNull(): Boolean? = if (isBoolean) asBoolean() else null

internal actual fun jsonBoolean(boolean: Boolean): JsonValue = jsonNodes.booleanNode(boolean)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
internal actual fun JsonValue.isNull(): Boolean = isNull

internal actual fun jsonNull(): JsonValue = jsonNodes.nullNode()

internal actual fun JsonValue.getField(key: String): JsonValue? {
    require(isObject) { "Value is expected to be an object" }
    return this[key]
}

public actual fun jsonObject(properties: Iterable<Pair<String, JsonValue>>): JsonValue {
    return ObjectNode(jsonNodes, properties.toMap(hashMapOf()))
}

internal actual operator fun JsonValue.set(property: String, value: JsonValue) {
    (this as ObjectNode)[property] = value
}

internal actual fun JsonValue.getFieldsOrNull(): Iterable<Map.Entry<String, JsonValue>>? = if (this is ObjectNode) {
    Iterable(this::fields)
} else null

internal actual fun jsonArray(vararg elements: JsonValue): JsonValue = ArrayNode(jsonNodes, elements.asList())

internal actual fun JsonValue.arrayElementsOrNull(): Iterable<JsonValue>? = if (isArray) this else null
