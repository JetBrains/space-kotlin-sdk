package space.jetbrains.api.runtime

internal expect fun secureRandomBytes(n: Int): Bytes

internal enum class CodeChallengeMethod(val parameterValue: String) {
    S256("S256"),
    PLAIN("plain"),
}

internal expect fun codeChallenge(codeVerifier: String): Pair<String, CodeChallengeMethod>
