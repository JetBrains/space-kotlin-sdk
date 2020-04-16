package space.jetbrains.api.runtime

import kotlin.browser.window

internal actual fun base64(src: String): String {
    return if (js("typeof window === 'object'") as Boolean) {
        // browser
        window.btoa(src)
    } else {
        // node
        js("Buffer").from(src).toString("base64") as String
    }
}
