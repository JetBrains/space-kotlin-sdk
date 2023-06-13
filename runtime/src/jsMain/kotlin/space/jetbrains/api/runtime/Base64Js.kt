package space.jetbrains.api.runtime

import kotlinx.browser.window
import org.khronos.webgl.Uint8Array

internal val isBrowser: Boolean = js("typeof window === 'object'") as Boolean

internal actual class Bytes(val value: String) {
    actual companion object {
        actual fun fromCharCodes(src: String): Bytes = Bytes(src)
    }
}

internal fun Uint8Array.toBytes(): Bytes {
    return Bytes.fromCharCodes(js("String.fromCharCode").apply(null, this) as String)
}

internal actual fun base64(src: Bytes): String {
    return if (isBrowser) {
        window.btoa(src.value)
    } else {
        // node
        js("Buffer").from(src, "latin1").toString("base64") as String
    }
}
