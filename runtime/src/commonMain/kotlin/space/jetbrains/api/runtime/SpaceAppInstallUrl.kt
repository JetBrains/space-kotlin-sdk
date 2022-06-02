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
 * @param authCodeFlow enables Auth Code flow for the application
 * @param authForMessagesFromSpace authentication for messages sent by Space to the app. Recommended value is [AuthForMessagesFromSpace.PUBLIC_KEY_SIGNATURE].
 * The [AuthForMessagesFromSpace.PUBLIC_KEY_SIGNATURE] is the default one and is not appended to URL parameters.
 */
public fun Space.appInstallUrl(
    spaceServerUrl: String,
    name: String,
    appEndpoint: String,
    state: String? = null,
    authCodeFlow: AuthCodeFlow? = null,
    authForMessagesFromSpace: AuthForMessagesFromSpace = AuthForMessagesFromSpace.PUBLIC_KEY_SIGNATURE,
): String = with(URLBuilder(spaceServerUrl)) {
    path("extensions", "installedApplications", "new")

    parameters.append("name", name)
    parameters.append("pair", "true")
    parameters.append("endpoint", appEndpoint)

    if (authCodeFlow != null) {
        parameters.append("code-flow-enabled", "true")

        val redirectUris = authCodeFlow.redirectUris.joinToString("\n")
        parameters.append("code-flow-redirect-uris", redirectUris)

        if (authCodeFlow.pkceRequired) {
            parameters.append("pkce-required", "true")
        }
    }

    state?.let { parameters.append("state", state) }

    if (authForMessagesFromSpace == AuthForMessagesFromSpace.SIGNING_KEY) {
        parameters.append("has-signing-key", "true")
    }

    build().toString()
}

public class AuthCodeFlow(internal val redirectUris: List<String>, internal val pkceRequired: Boolean)

public enum class AuthForMessagesFromSpace {
    PUBLIC_KEY_SIGNATURE,
    SIGNING_KEY
}
