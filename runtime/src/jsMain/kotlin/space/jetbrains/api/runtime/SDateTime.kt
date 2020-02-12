package space.jetbrains.api.runtime

import kotlin.js.*

actual class SDateTime actual constructor(actual val timestamp: Long) {
    actual val iso = Date(timestamp).toISOString()

    actual override fun toString(): String = iso
}
