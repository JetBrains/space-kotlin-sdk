package space.jetbrains.api.runtime

import io.ktor.http.Parameters
import kotlinx.datetime.*
import kotlinx.datetime.Clock.System

public interface TokenInfo {
    public val accessToken: String
    public val expires: Instant?
}

public interface TokenSource {
    public suspend fun token(): TokenInfo
}

public data class ExpiringToken(override val accessToken: String, override val expires: Instant) : TokenInfo

public data class PermanentToken(override val accessToken: String) : TokenInfo, TokenSource {
    override val expires: Instant? get() = null
    override suspend fun token(): TokenInfo = this
}

private fun TokenInfo.expired(gapSeconds: Long = 5): Boolean {
    return expires?.let { System.now().plus(gapSeconds, DateTimeUnit.SECOND) > it } ?: false
}

public class ExpiringTokenSource(private val getToken: suspend () -> TokenInfo) : TokenSource {
    private var currentToken: TokenInfo? = null

    override suspend fun token(): TokenInfo {
        val current = currentToken

        return if (current?.expired() != false) {
            getToken().also { currentToken = it }
        } else current
    }
}

public class ServiceAccountTokenSource(
    spaceClient: SpaceHttpClient,
    clientId: String,
    clientSecret: String,
    serverUrl: String,
    scope: String = "**"
) : TokenSource by ExpiringTokenSource(getToken = {
    spaceClient.auth(
        url = SpaceServerLocation(serverUrl).oauthUrl,
        methodBody = Parameters.build {
            append("grant_type", "client_credentials")
            append("scope", scope)
        },
        authHeaderValue = "Basic " + base64("$clientId:$clientSecret")
    )
})

public fun SpaceHttpClient.withPermanentToken(token: String, serverUrl: String): SpaceHttpClientWithCallContext =
    withCallContext(serverUrl, PermanentToken(token))

public fun SpaceHttpClient.withServiceAccountTokenSource(
    clientId: String,
    clientSecret: String,
    serverUrl: String,
    scope: String = "**"
): SpaceHttpClientWithCallContext = withCallContext(SpaceHttpClientCallContext(
    serverUrl = serverUrl,
    tokenSource = ServiceAccountTokenSource(this, clientId, clientSecret, serverUrl, scope)
))