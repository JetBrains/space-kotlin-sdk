package space.jetbrains.api.runtime

actual class SDate actual constructor(actual val iso: String) {
    actual override fun toString(): String = iso
}
