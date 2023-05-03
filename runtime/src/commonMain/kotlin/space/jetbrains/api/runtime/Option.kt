package space.jetbrains.api.runtime

/**
 * This class represents a value which can be absent. Specifying [Option.None] (or omitting the parameter) will leave
 * out the parameter from the request. Specifying `Option.Value(value)` will send the enclosed value as the parameter
 * value.
 *
 * Example:
 * ```
 * suspend fun endpoint(parameter: Option<String?> = Option.None)
 *
 * endpoint()                                     // resulting JSON: {}
 * endpoint(parameter = Option.Value("value 1"))  // resulting JSON: {"parameter":"value1"}
 * endpoint(parameter = Option.Value(null)        // resulting JSON: {"parameter":null}
 * ```
 */
public sealed class Option<out T> {
    public data class Value<out T>(val value: T) : Option<T>()
    public object None : Option<Nothing>()
}

public val <T> Option<T>.valueOrNull: T? get() = (this as? Option.Value)?.value

public inline fun <T, R> Option<T>.ifValue(then: (T) -> R): R? {
    return if (this is Option.Value) then(value) else null
}
