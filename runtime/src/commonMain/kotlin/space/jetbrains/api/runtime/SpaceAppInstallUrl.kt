package space.jetbrains.api.runtime

import io.ktor.http.*

/**
 * Creates a URL for installing app to particular Space organization.
 *
 * @param spaceServerUrl HTTPS url for Space organization, for example: `https://my-org.jetbrains.space`
 * @param name Default application name. Can be changed by users in each Space organization.
 * @param appEndpoint HTTPS url that Space will use to send messages to the app
 * @param state a string that will be passed to the application in `InitPayload` when user installs the app.
 * Allows to track the installation process across different systems while user is redirected in the browser.
 * @param authFlows authentication flows that application will use to access Space API
 * @param authForMessagesFromSpace authentication for messages sent by Space to the app. Recommended value is [AuthForMessagesFromSpace.PUBLIC_KEY_SIGNATURE]
 */
public fun Space.appInstallUrl(
    spaceServerUrl: String,
    name: String,
    appEndpoint: String,
    state: String? = null,
    authFlows: Set<SpaceAuthFlow> = setOf(SpaceAuthFlow.ClientCredentials),
    authForMessagesFromSpace: AuthForMessagesFromSpace = AuthForMessagesFromSpace.PUBLIC_KEY_SIGNATURE,
): String = with(URLBuilder(spaceServerUrl)) {
    path("extensions", "installedApplications", "new")

    parameters.append("name", name)
    parameters.append("pair", "true")
    parameters.append("endpoint", appEndpoint)

    authFlows.forEach { authFlow ->
        when (authFlow) {
            is SpaceAuthFlow.ClientCredentials -> {
                parameters.append("client-credentials-flow-enabled", "true")
            }
            is SpaceAuthFlow.AuthorizationCode -> {
                parameters.append("code-flow-enabled", "true")

                val redirectUris = authFlow.redirectUris.joinToString("\n")
                parameters.append("code-flow-redirect-uris", redirectUris)

                if (authFlow.pkceRequired) {
                    parameters.append("pkce-required", "true")
                }
            }
        }
    }

    state?.let { parameters.append("state", state) }

    when (authForMessagesFromSpace) {
        AuthForMessagesFromSpace.PUBLIC_KEY_SIGNATURE -> parameters.append("has-public-key-signature", "true")
        AuthForMessagesFromSpace.SIGNING_KEY -> parameters.append("has-signing-key", "true")
    }

    build().toString()
}

public sealed class SpaceAuthFlow {
    public object ClientCredentials : SpaceAuthFlow()
    public class AuthorizationCode(internal val redirectUris: List<String>, internal val pkceRequired: Boolean) :
        SpaceAuthFlow()
}

public enum class AuthForMessagesFromSpace {
    PUBLIC_KEY_SIGNATURE,
    SIGNING_KEY
}
