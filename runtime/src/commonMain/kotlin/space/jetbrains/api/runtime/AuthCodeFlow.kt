package space.jetbrains.api.runtime

import io.ktor.client.HttpClient
import io.ktor.http.*

/**
 * Values for the request_credentials parameter:
 * See [Space documentation](https://www.jetbrains.com/help/space/authorization-code.html#parameters)
 */
public enum class OAuthRequestCredentials(internal val parameterValue: String) {
    /**
     * Logs the user out of Space and redirects them to the login page.
     * Use as a response to a logout request in the application.
     */
    REQUIRED("required"),

    /**
     * Use when the application does not allow anonymous access.
     * - If the user is already logged in to Space, the user is granted access to the application.
     * - If the user is not logged in to Space, the user is redirected to the login page.
     */
    DEFAULT("default"),
}

/**
 * Indicates whether the application requires access to Space when the user is not online.
 * If the application requires refreshing access tokens when the user is not online, use the [OFFLINE] value.
 * In this case Space issues a refresh token for the application the first time it exchanges an authorization code for a
 * user.
 * See Space documentation on
 * [Refresh Token Flow](https://www.jetbrains.com/help/space/refresh-token.html).
 */
public enum class OAuthAccessType(internal val parameterValue: String) {
    ONLINE("online"), OFFLINE("offline")
}

/**
 * An object to hold helper functions defined by the SDK.
 * Note that some helpers are also defined as extension on this object.
 */
public object Space {
    /**
     * Returns URL with authorization request.
     * Redirect the user to it in order to receive authorization code (when the user gives their consent).
     * See Space documentation for more information on
     * [Authorization Code Flow](https://www.jetbrains.com/help/space/authorization-code.html)
     * and for
     * [details on parameters](https://www.jetbrains.com/help/space/authorization-code.html#parameters).
     *
     * @param scope is a space-separated list of rights required to access specific resources in Space.
     * @param state An identifier for the current application state.
     * The value will be passed the application after the authorization.
     * @param redirectUri A URI in your application that can handle responses from Space. If it is not specified,
     * the first redirect URI from application configuration in Space will be used.
     * @param requestCredentials specifies in which cases the login form should be shown to the user.
     * @param accessType indicates whether the application requires access to Space when the user is not online.
     */
    public fun authCodeSpaceUrl(
        appInstance: SpaceAppInstance,
        scope: PermissionScope,
        state: String? = null,
        redirectUri: String? = null,
        requestCredentials: OAuthRequestCredentials? = null,
        accessType: OAuthAccessType = OAuthAccessType.ONLINE,
    ): String = URLBuilder(appInstance.spaceServer.oauthAuthUrl).also {
        it.parameters.append("response_type", "code")
        if (state != null) {
            it.parameters.append("state", state)
        }
        if (redirectUri != null) {
            it.parameters.append("redirect_uri", redirectUri)
        }
        if (requestCredentials != null) {
            it.parameters.append("request_credentials", requestCredentials.parameterValue)
        }
        it.parameters.append("client_id", appInstance.clientId)
        it.parameters.append("scope", scope.toString())
        it.parameters.append("access_type", accessType.parameterValue)
    }.build().toString()

    @Deprecated(
        "Use PermissionScope",
        ReplaceWith(
            "Space.authCodeSpaceUrl(appInstance, PermissionScope.fromString(scope), state, redirectUri, requestCredentials, accessType)",
        )
    )
    public fun authCodeSpaceUrl(
        appInstance: SpaceAppInstance,
        scope: String,
        state: String? = null,
        redirectUri: String? = null,
        requestCredentials: OAuthRequestCredentials? = null,
        accessType: OAuthAccessType = OAuthAccessType.ONLINE,
    ): String = authCodeSpaceUrl(appInstance, PermissionScope.fromString(scope), state, redirectUri, requestCredentials, accessType)

    // TODO PKCE
    /**
     * Exchanges authorization code for an access token. This can be performed only once per each code.
     * See Space documentation on
     * [Authorization Code Flow](https://www.jetbrains.com/help/space/authorization-code.html).
     *
     * If during authorization code request [OAuthAccessType] was specified as [OAuthAccessType.OFFLINE],
     * resulting [SpaceTokenInfo] will also contain a refresh token.
     * See Space documentation on
     * [Refresh Token Flow](https://www.jetbrains.com/help/space/refresh-token.html).
     *
     * If [redirectUri] is missing, the first redirect URI from application configuration in Space will be used.
     */
    public suspend fun exchangeAuthCodeForToken(
        ktorClient: HttpClient,
        appInstance: SpaceAppInstance,
        authCode: String,
        redirectUri: String? = null
    ): SpaceTokenInfo = auth(
        ktorClient = ktorClient,
        url = appInstance.spaceServer.oauthTokenUrl,
        methodBody = Parameters.build {
            append("grant_type", "authorization_code")
            append("code", authCode)
            if (redirectUri != null) {
                append("redirect_uri", redirectUri)
            }
        },
        authHeaderValue = "Basic " + base64("${appInstance.clientId}:${appInstance.clientSecret}")
    )
}
