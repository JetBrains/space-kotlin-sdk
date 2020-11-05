package space.jetbrains.api.runtime

public sealed class Option<out T> {
    public data class Value<out T>(val value: T) : Option<T>()
    public object None : Option<Nothing>()
}

public val <T> Option<T>.valueOrNull: T? get() = (this as? Option.Value)?.value

public inline fun <T, R> Option<T>.ifValue(then: (T) -> R): R? {
    return if (this is Option.Value) then(value) else null
}
