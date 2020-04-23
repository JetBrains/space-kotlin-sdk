package space.jetbrains.api.runtime

import io.ktor.http.Parameters

interface TokenInfo {
    val accessToken: String
    val expires: SDateTime?
}

interface TokenSource {
    suspend fun token(): TokenInfo
}

data class ExpiringToken(override val accessToken: String, override val expires: SDateTime) : TokenInfo

data class PermanentToken(override val accessToken: String) : TokenInfo, TokenSource {
    override val expires: SDateTime? get() = null
    override suspend fun token(): TokenInfo = this
}

private fun TokenInfo.expired(gapSeconds: Long = 5): Boolean {
    return expires?.let { now.plusSeconds(gapSeconds) < it } ?: false
}

class ExpiringTokenSource(private val getToken: suspend () -> TokenInfo) : TokenSource {
    private var currentToken: TokenInfo? = null

    override suspend fun token(): TokenInfo {
        val current = currentToken

        return if (current?.expired() != false) {
            getToken().also { currentToken = it }
        } else current
    }
}

class ServiceAccountTokenSource(
    spaceClient: SpaceHttpClient,
    clientId: String,
    clientSecret: String,
    serverUrl: String
) : TokenSource by ExpiringTokenSource(getToken = {
    spaceClient.auth(
        url = SpaceServerLocation(serverUrl).oauthUrl,
        methodBody = Parameters.build {
            append("grant_type", "client_credentials")
        },
        authHeaderValue = "Basic " + base64("$clientId:$clientSecret")
    )
})

fun SpaceHttpClient.withPermanentToken(token: String, serverUrl: String): SpaceHttpClientWithCallContext =
    withCallContext(serverUrl, PermanentToken(token))

fun SpaceHttpClient.withServiceAccountTokenSource(
    clientId: String,
    clientSecret: String,
    serverUrl: String
): SpaceHttpClientWithCallContext = withCallContext(SpaceHttpClientCallContext(
    serverUrl = serverUrl,
    tokenSource = ServiceAccountTokenSource(this, clientId, clientSecret, serverUrl)
))