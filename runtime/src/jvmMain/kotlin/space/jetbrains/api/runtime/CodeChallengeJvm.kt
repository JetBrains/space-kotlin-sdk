package space.jetbrains.api.runtime

import java.security.MessageDigest
import java.security.SecureRandom

private fun codeChallengeBytes(codeVerifier: String): Bytes {
    val bytes = MessageDigest.getInstance("SHA-256")
        .also { it.update(codeVerifier.toByteArray(Charsets.UTF_8)) }
        .digest()
    return Bytes(bytes)
}

private val secureRandom by lazy { SecureRandom() }

internal actual fun codeChallenge(codeVerifier: String): Pair<String, CodeChallengeMethod> {
    return base64UrlSafe(codeChallengeBytes(codeVerifier)) to CodeChallengeMethod.S256
}

internal actual fun secureRandomBytes(n: Int): Bytes {
    val array = ByteArray(n)
    secureRandom.nextBytes(array)
    return Bytes(array)
}
