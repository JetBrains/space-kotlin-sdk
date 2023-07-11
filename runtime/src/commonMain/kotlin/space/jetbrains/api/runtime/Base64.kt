package space.jetbrains.api.runtime

internal fun SpaceAppInstance.basicAuthHeaderValue(): String? = if (clientSecretOrNull != null) {
    "Basic " + base64(Bytes.fromCharCodes("$clientId:$clientSecretOrNull"))
} else null

internal expect class Bytes {
    companion object {
        fun fromCharCodes(src: String): Bytes
    }
}

internal fun base64UrlSafe(src: Bytes): String = base64(src)
    .replace("=", "")
    .replace("+", "-")
    .replace("/", "_")

internal expect fun base64(src: Bytes): String
