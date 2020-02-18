package space.jetbrains.api.runtime

actual abstract class JsonValue

internal actual fun parseJson(json: String): JsonValue? {
    TODO("Not yet implemented")
}

internal actual fun JsonValue.print(): String {
    TODO("Not yet implemented")
}

internal actual fun jsonNumber(number: Number): JsonValue {
    TODO("Not yet implemented")
}

internal actual fun JsonValue.asNumberOrNull(): Number? {
    TODO("Not yet implemented")
}

internal actual fun jsonString(string: String): JsonValue {
    TODO("Not yet implemented")
}

internal actual fun JsonValue.asStringOrNull(): String? {
    TODO("Not yet implemented")
}

internal actual fun jsonBoolean(boolean: Boolean): JsonValue {
    TODO("Not yet implemented")
}

internal actual fun JsonValue.asBooleanOrNull(): Boolean? {
    TODO("Not yet implemented")
}

internal actual fun jsonNull(): JsonValue {
    TODO("Not yet implemented")
}

internal actual fun JsonValue.isNull(): Boolean {
    TODO("Not yet implemented")
}

actual fun jsonObject(properties: Iterable<Pair<String, JsonValue>>): JsonValue {
    TODO("Not yet implemented")
}

internal actual operator fun JsonValue.get(key: String): JsonValue? {
    TODO("Not yet implemented")
}

internal actual operator fun JsonValue.set(property: String, value: JsonValue) {
}

internal actual fun jsonArray(vararg elements: JsonValue): JsonValue {
    TODO("Not yet implemented")
}

internal actual fun JsonValue.arrayElementsOrNull(): Iterable<JsonValue>? {
    TODO("Not yet implemented")
}