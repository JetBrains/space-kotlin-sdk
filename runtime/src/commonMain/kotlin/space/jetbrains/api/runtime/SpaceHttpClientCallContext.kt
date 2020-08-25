package space.jetbrains.api.runtime

public class SpaceHttpClientCallContext(serverUrl: String, public val tokenSource: TokenSource) {
    public val server: SpaceServerLocation = SpaceServerLocation(serverUrl)
}

public class SpaceHttpClientWithCallContext(public val client: SpaceHttpClient, public val callContext: SpaceHttpClientCallContext)

public fun SpaceHttpClient.withCallContext(callContext: SpaceHttpClientCallContext): SpaceHttpClientWithCallContext =
    SpaceHttpClientWithCallContext(this, callContext)

public fun SpaceHttpClient.withCallContext(serverUrl: String, tokenSource: TokenSource): SpaceHttpClientWithCallContext =
    withCallContext(SpaceHttpClientCallContext(serverUrl, tokenSource))

public class SpaceServerLocation(serverUrl: String) {
    public val apiBaseUrl: String = "${serverUrl.trimEnd('/')}/api/http/"
    public val oauthUrl: String = "${serverUrl.trimEnd('/')}/oauth/token"
}
