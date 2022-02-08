package space.jetbrains.api.runtime

import kotlinx.browser.window

internal actual fun base64(src: String): String {
    return if (js("typeof window === 'object'") as Boolean) {
        // browser
        window.btoa(src)
    } else {
        // node
        js("Buffer").from(src, "latin1").toString("base64") as String
    }
}
