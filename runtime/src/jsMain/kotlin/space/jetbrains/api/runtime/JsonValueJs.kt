package space.jetbrains.api.runtime

actual abstract external class JsonValue

internal actual fun parseJson(json: String): JsonValue? = json.takeIf { it.isNotEmpty() }?.let(JSON::parse)
internal actual fun JsonValue.print(): String = JSON.stringify(this)

private fun Any?.asJsonValue(): JsonValue = unsafeCast<JsonValue>()

internal actual fun JsonValue.asNumberOrNull(): Number? = asDynamic() as? Number
internal actual fun jsonNumber(number: Number): JsonValue = number.asJsonValue()

internal actual fun JsonValue.asStringOrNull(): String? = asDynamic() as? String
internal actual fun jsonString(string: String): JsonValue = string.asJsonValue()

internal actual fun JsonValue.asBooleanOrNull(): Boolean? = asDynamic() as? Boolean
internal actual fun jsonBoolean(boolean: Boolean): JsonValue = boolean.asJsonValue()

internal actual fun jsonNull(): JsonValue = null.asJsonValue()
internal actual fun JsonValue.isNull(): Boolean = asDynamic() == null

internal actual operator fun JsonValue.get(key: String): JsonValue? {
    require(jsTypeOf(this) == "object")
    return asDynamic()[key]?.unsafeCast<JsonValue>()
}

actual fun jsonObject(properties: Iterable<Pair<String, JsonValue>>): JsonValue {
    val result = js("{}")
    for ((key, value) in properties) {
        result[key] = value
    }
    return result.unsafeCast<JsonValue>()
}

internal actual operator fun JsonValue.set(property: String, value: JsonValue) {
    asDynamic()[property] = value
}

internal actual fun jsonArray(vararg elements: JsonValue): JsonValue = arrayOf(elements).asJsonValue()

internal actual fun JsonValue.arrayElementsOrNull(): Iterable<JsonValue>? {
    return if (jsTypeOf(this) == "array") unsafeCast<Array<JsonValue>>().asIterable() else null
}
