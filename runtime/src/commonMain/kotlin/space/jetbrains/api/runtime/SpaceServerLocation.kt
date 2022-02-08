package space.jetbrains.api.runtime

public class SpaceServerLocation(serverUrl: String) {
    public val serverUrl: String = serverUrl.trimEnd('/')
    public val apiBaseUrl: String = "${this.serverUrl}/api/http/"
    public val oauthTokenUrl: String
    public val oauthAuthUrl: String

    init {
        val oauthUrl = "${this.serverUrl}/oauth"
        oauthTokenUrl = "$oauthUrl/token"
        oauthAuthUrl = "$oauthUrl/auth"
    }
}