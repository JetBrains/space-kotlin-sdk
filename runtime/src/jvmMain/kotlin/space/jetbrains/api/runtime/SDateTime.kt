package space.jetbrains.api.runtime

import org.joda.time.*

actual class SDateTime actual constructor(actual val timestamp: Long) {
    val joda = DateTime(timestamp)
    actual val iso: String = joda.toString()

    actual override fun toString(): String = iso
}
