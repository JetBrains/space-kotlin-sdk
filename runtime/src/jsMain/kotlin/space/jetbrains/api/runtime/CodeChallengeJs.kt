package space.jetbrains.api.runtime

import org.khronos.webgl.Uint8Array

private val crypto: dynamic = if (isBrowser) {
    js("((typeof global !== 'undefined') ? global : self)").crypto
} else {
    js("require(\"crypto\")")
}

internal actual fun codeChallenge(codeVerifier: String): Pair<String, CodeChallengeMethod> {
    return codeVerifier to CodeChallengeMethod.PLAIN
}

internal actual fun secureRandomBytes(n: Int): Bytes {
    val array = Uint8Array(n)
    crypto.getRandomValues(array)
    return array.toBytes()
}
