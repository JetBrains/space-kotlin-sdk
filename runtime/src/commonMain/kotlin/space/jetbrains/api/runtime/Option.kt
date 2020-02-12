package space.jetbrains.api.runtime

sealed class Option<out T> {
    data class Value<out T>(val value: T) : Option<T>()
    object None : Option<Nothing>()
}
