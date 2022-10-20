package space.jetbrains.api.runtime

import io.ktor.client.*
import io.ktor.http.*
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
        scope: PermissionScope = PermissionScope.All,
    ) : SpaceAuth by expiringTokenSourceAuth(getToken = { spaceClient, appInstance ->
        auth(
            ktorClient = spaceClient,
            url = appInstance.spaceServer.oauthTokenUrl,
            methodBody = Parameters.build {
                append("grant_type", "client_credentials")
                append("scope", scope.toString())
            },
            authHeaderValue = "Basic " + base64("${appInstance.clientId}:${appInstance.clientSecret}"),
        )
    }) {
        @Deprecated(
            "Use PermissionScope",
            ReplaceWith("SpaceAuth.ClientCredentials(PermissionScope.fromString(scope))")
        )
        public constructor(scope: String) : this(PermissionScope.fromString(scope))
    }

    public class RefreshToken(
        refreshToken: String,
        scope: PermissionScope,
    ) : SpaceAuth by expiringTokenSourceAuth(getToken = { spaceClient, appInstance ->
        auth(
            ktorClient = spaceClient,
            url = appInstance.spaceServer.oauthTokenUrl,
            methodBody = Parameters.build {
                append("grant_type", "refresh_token")
                append("refresh_token", value = refreshToken)
                append("scope", value = scope.toString())
            },
            authHeaderValue = "Basic " + base64("${appInstance.clientId}:${appInstance.clientSecret}"),
        )
    }) {
        @Deprecated(
            "Use PermissionScope",
            ReplaceWith("SpaceAuth.RefreshToken(refreshToken, PermissionScope.fromString(scope))")
        )
        public constructor(refreshToken: String, scope: String) : this(refreshToken, PermissionScope.fromString(scope))
    }
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
    ReplaceWith("SpaceClient(this, serverUrl = serverUrl, token = token)"),
)
public fun HttpClient.withPermanentToken(token: String, serverUrl: String): SpaceClient =
    SpaceClient(this, serverUrl = serverUrl, token = token)

@Deprecated(
    "Create SpaceClient with SpaceAuth.ClientCredentials",
    ReplaceWith(
        "SpaceClient(this, SpaceAppInstance(clientId, clientSecret, serverUrl), " +
                "SpaceAuth.ClientCredentials(PermissionScope.fromString(scope)))",
    ),
)
public fun HttpClient.withServiceAccountTokenSource(
    clientId: String,
    clientSecret: String,
    serverUrl: String,
    scope: String = "**",
): SpaceClient = SpaceClient(
    ktorClient = this,
    appInstance = SpaceAppInstance(clientId, clientSecret, serverUrl),
    auth = SpaceAuth.ClientCredentials(PermissionScope.fromString(scope))
)

@Deprecated("Use SpaceAuth", ReplaceWith("SpaceAuth"))
public typealias TokenSource = SpaceAuth
