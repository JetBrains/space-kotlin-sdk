package space.jetbrains.api.runtime

import java.util.Base64

internal actual fun base64(src: String): String = Base64.getEncoder().encodeToString(src.toByteArray())
