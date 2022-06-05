import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import space.jetbrains.api.runtime.AuthForMessagesFromSpace
import space.jetbrains.api.runtime.Space
import space.jetbrains.api.runtime.SpaceAuthFlow
import space.jetbrains.api.runtime.appInstallUrl

class AppInstallUrlTest {
    @Test
    fun basicUrl() {
        val url = Space.appInstallUrl(
            spaceServerUrl = "https://my-org.jetbrains.space/",
            name = "My app",
            appEndpoint = "https://my-server.domain.com/api",
        )
        assertThat(url).isEqualTo(
            "https://my-org.jetbrains.space/extensions/installedApplications/new?" +
                    "name=My+app" +
                    "&pair=true" +
                    "&endpoint=https%3A%2F%2Fmy-server.domain.com%2Fapi" +
                    "&client-credentials-flow-enabled=true" +
                    "&has-public-key-signature=true"
        )
    }

    @Test
    fun advancedConfig() {
        val url = Space.appInstallUrl(
            spaceServerUrl = "https://my-org.jetbrains.space/",
            name = "My app",
            appEndpoint = "https://my-server.domain.com/api",
            state = "4e617c52-3906-4ad6-ac35-5be3fe66608b",
            authFlows = setOf(
                SpaceAuthFlow.ClientCredentials,
                SpaceAuthFlow.AuthorizationCode(
                    redirectUris = listOf(
                        "https://server1.domain.com/redirect-auth1",
                        "https://server2.domain.com/redirect-auth2"
                    ), pkceRequired = true
                )
            ),
            authForMessagesFromSpace = AuthForMessagesFromSpace.SIGNING_KEY
        )

        assertThat(url).isEqualTo(
            "https://my-org.jetbrains.space/extensions/installedApplications/new?" +
                    "name=My+app" +
                    "&pair=true" +
                    "&endpoint=https%3A%2F%2Fmy-server.domain.com%2Fapi" +
                    "&client-credentials-flow-enabled=true" +
                    "&code-flow-enabled=true" +
                    "&code-flow-redirect-uris=https%3A%2F%2Fserver1.domain.com%2Fredirect-auth1%0Ahttps%3A%2F%2Fserver2.domain.com%2Fredirect-auth2" +
                    "&pkce-required=true" +
                    "&state=4e617c52-3906-4ad6-ac35-5be3fe66608b" +
                    "&has-signing-key=true"
        )
    }
}
