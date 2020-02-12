package space.jetbrains.api.runtime

interface SimpleTokenInfo {
    val accessToken: String
}

interface SimpleTokenSource {
    suspend fun token(): SimpleTokenInfo
    fun lastToken(): SimpleTokenInfo
}

class OneTimeSimpleTokenSource<out T : SimpleTokenInfo>(private val acquireToken: suspend () -> T) : SimpleTokenSource {
    private var currentToken: T? = null

    override suspend fun token(): T = currentToken ?: acquireToken().also {
        currentToken = it
    }

    override fun lastToken(): T = currentToken ?: error("token was not acquired yet")
}
