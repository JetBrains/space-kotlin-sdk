package space.jetbrains.api.runtime

import java.nio.CharBuffer
import java.util.Base64

internal actual fun base64(src: Bytes): String = Base64.getEncoder().encodeToString(src.value)

internal actual class Bytes(val value: ByteArray) {
    actual companion object {
        actual fun fromCharCodes(src: String): Bytes {
            return Bytes(Charsets.ISO_8859_1.newEncoder().encode(CharBuffer.wrap(src)).array())
        }
    }
}
