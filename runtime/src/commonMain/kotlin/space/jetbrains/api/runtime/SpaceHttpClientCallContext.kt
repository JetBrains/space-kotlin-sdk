package space.jetbrains.api.runtime

class SpaceHttpClientCallContext(serverUrl: String, val tokenSource: TokenSource) {
    val server = SpaceServerLocation(serverUrl)
}

class SpaceHttpClientWithCallContext(val client: SpaceHttpClient, val callContext: SpaceHttpClientCallContext)

fun SpaceHttpClient.withCallContext(callContext: SpaceHttpClientCallContext): SpaceHttpClientWithCallContext =
    SpaceHttpClientWithCallContext(this, callContext)

fun SpaceHttpClient.withCallContext(serverUrl: String, tokenSource: TokenSource): SpaceHttpClientWithCallContext =
    withCallContext(SpaceHttpClientCallContext(serverUrl, tokenSource))

class SpaceServerLocation(serverUrl: String) {
    val apiBaseUrl = "${serverUrl.trimEnd('/')}/api/http/"
    val oauthUrl = "${serverUrl.trimEnd('/')}/oauth/token"
}
