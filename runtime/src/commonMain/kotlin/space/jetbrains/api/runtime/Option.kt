package space.jetbrains.api.runtime

public sealed class Option<out T> {
    public data class Value<out T>(val value: T) : Option<T>()
    public object None : Option<Nothing>()
}
