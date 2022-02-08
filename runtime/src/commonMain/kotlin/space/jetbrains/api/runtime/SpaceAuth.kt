package space.jetbrains.api.runtime

import io.ktor.client.HttpClient
import io.ktor.http.Parameters
import kotlinx.datetime.Instant

public interface SpaceAuth {
    public suspend fun token(client: HttpClient, appInstance: SpaceAppInstance): SpaceTokenInfo

    public class Token private constructor(
        private val token: SpaceTokenInfo,
        private val gapSecondsForExpiredCheck: Long = 5,
    ) : SpaceAuth {
        public constructor(accessToken: String) : this(SpaceTokenInfo(accessToken, expires = null))

        public constructor(
            accessToken: String,
            expires: Instant? = null,
            gapSecondsForExpiredCheck: Long = 5,
        ) : this(SpaceTokenInfo(accessToken, expires), gapSecondsForExpiredCheck)

        /** If token is expired, throws [TokenExpiredException] */
        override suspend fun token(client: HttpClient, appInstance: SpaceAppInstance): SpaceTokenInfo {
            if (token.expired(gapSeconds = gapSecondsForExpiredCheck)) throw TokenExpiredException()
            return token
        }
    }

    public class ClientCredentials(
        scope: String = "**",
    ) : SpaceAuth by expiringTokenSourceAuth(getToken = { spaceClient, appInstance ->
        auth(
            ktorClient = spaceClient,
            url = appInstance.spaceServer.oauthTokenUrl,
            methodBody = Parameters.build {
                append("grant_type", "client_credentials")
                append("scope", scope)
            },
            authHeaderValue = "Basic " + base64("${appInstance.clientId}:${appInstance.clientSecret}"),
        )
    })

    public class RefreshToken(
        refreshToken: String,
        scope: String,
    ) : SpaceAuth by expiringTokenSourceAuth(getToken = { spaceClient, appInstance ->
        auth(
            ktorClient = spaceClient,
            url = appInstance.spaceServer.oauthTokenUrl,
            methodBody = Parameters.build {
                append("grant_type", "refresh_token")
                append("refresh_token", value = refreshToken)
                append("scope", value = scope)
            },
            authHeaderValue = "Basic " + base64("${appInstance.clientId}:${appInstance.clientSecret}"),
        )
    })
}

public class TokenExpiredException : Exception()

public fun expiringTokenSourceAuth(getToken: suspend (HttpClient, SpaceAppInstance) -> SpaceTokenInfo): SpaceAuth {
    var currentToken: SpaceTokenInfo? = null

    return object : SpaceAuth {
        override suspend fun token(client: HttpClient, appInstance: SpaceAppInstance): SpaceTokenInfo {
            val current = currentToken

            return if (current?.expired() != false) {
                getToken(client, appInstance).also { currentToken = it }
            } else current
        }
    }
}

@Deprecated(
    "Create SpaceClient with SpaceAuth.Token",
    ReplaceWith(
        "SpaceClient(this, serverUrl = serverUrl, token = token)",
        "space.jetbrains.api.runtime.SpaceClient"
    ),
)
public fun HttpClient.withPermanentToken(token: String, serverUrl: String): SpaceClient =
    SpaceClient(this, serverUrl = serverUrl, token = token)

@Deprecated(
    "Create SpaceClient with SpaceAuth.ClientCredentials",
    ReplaceWith(
        "SpaceClient(this, InstalledApp(clientId, clientSecret, serverUrl), SpaceAuth.ClientCredentials(scope))",
        "space.jetbrains.api.runtime.SpaceClient",
        "space.jetbrains.api.runtime.InstalledApp"
    ),
)
public fun HttpClient.withServiceAccountTokenSource(
    clientId: String,
    clientSecret: String,
    serverUrl: String,
    scope: String = "**",
): SpaceClient = SpaceClient(this, SpaceAppInstance(clientId, clientSecret, serverUrl), SpaceAuth.ClientCredentials(scope))

@Deprecated("Use SpaceAuth", ReplaceWith("SpaceAuth"))
public typealias TokenSource = SpaceAuth
