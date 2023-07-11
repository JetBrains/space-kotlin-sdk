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
     * @param scope is a collection of permissions required to access specific resources in Space.
     * @param state This value will be added as a parameter to redirectUri by Space.
     * Your application should use this to identify and continue the authorization process.
     * @param redirectUri Space redirects user to this URI after the authorization.
     * Your application should handle the request and extract the auth code from parameters.
     * @param requestCredentials specifies whether to show the authorization form to the user each time,
     * even when the consent has already been given.
     * @param accessType indicates whether the application requires access to Space when the user is not online.
     * Specify [OAuthAccessType.OFFLINE] if you need to receive a refresh token.
     * @param codeVerifier if you'd like to use PKCE extension, pass a high-entropy cryptographic random string that
     * uses the unreserved characters [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~", with a minimum length of 43 characters and a maximum length of
     * 128 characters. Can be generated using [Space.generateCodeVerifier].
     *
     * The proof key for code exchange (PKCE) is an additional protection code that further enhances the authorization
     * flow. [Read more](https://www.jetbrains.com/help/space/authorization-code.html#basics).
     */
    public fun authCodeSpaceUrl(
        appInstance: SpaceAppInstance,
        scope: PermissionScope,
        state: String? = null,
        redirectUri: String,
        requestCredentials: OAuthRequestCredentials? = null,
        accessType: OAuthAccessType = OAuthAccessType.ONLINE,
        codeVerifier: String? = null,
    ): String = URLBuilder(appInstance.spaceServer.oauthAuthUrl).also {
        it.parameters.append("response_type", "code")
        if (state != null) {
            it.parameters.append("state", state)
        }
        it.parameters.append("redirect_uri", redirectUri)
        if (requestCredentials != null) {
            it.parameters.append("request_credentials", requestCredentials.parameterValue)
        }
        it.parameters.append("client_id", appInstance.clientId)
        it.parameters.append("scope", scope.toString())
        it.parameters.append("access_type", accessType.parameterValue)
        if (codeVerifier != null) {
            val (codeChallenge, codeChallengeMethod) = codeChallenge(codeVerifier)
            it.parameters.append("code_challenge_method", codeChallengeMethod.parameterValue)
            it.parameters.append("code_challenge", codeChallenge)
        }
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
        redirectUri: String,
        requestCredentials: OAuthRequestCredentials? = null,
        accessType: OAuthAccessType = OAuthAccessType.ONLINE,
    ): String = authCodeSpaceUrl(appInstance, PermissionScope.fromString(scope), state, redirectUri, requestCredentials, accessType)

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
     * If you use PKCE extension, and `codeVerifier` was specified during authorization code request
     * (in [authCodeSpaceUrl]), [codeVerifier] parameter of this function must have the same value.
     *
     * The proof key for code exchange (PKCE) is an additional protection code that further enhances the authorization
     * flow. [Read more](https://www.jetbrains.com/help/space/authorization-code.html#basics).
     */
    public suspend fun exchangeAuthCodeForToken(
        ktorClient: HttpClient,
        appInstance: SpaceAppInstance,
        authCode: String,
        redirectUri: String,
        codeVerifier: String? = null,
    ): SpaceTokenInfo = auth(
        ktorClient = ktorClient,
        url = appInstance.spaceServer.oauthTokenUrl,
        methodBody = Parameters.build {
            append("grant_type", "authorization_code")
            append("code", authCode)
            append("redirect_uri", redirectUri)
            if (codeVerifier != null) {
                append("code_verifier", codeVerifier)
            }
            if (appInstance.clientSecretOrNull == null) {
                append("client_id", appInstance.clientId)
            }
        },
        authHeaderValue = appInstance.basicAuthHeaderValue(),
    )

    /** Generates `codeVerifier` for use in [authCodeSpaceUrl] and, subsequently, in [exchangeAuthCodeForToken] */
    public fun generateCodeVerifier(): String = base64UrlSafe(secureRandomBytes(32))
}
