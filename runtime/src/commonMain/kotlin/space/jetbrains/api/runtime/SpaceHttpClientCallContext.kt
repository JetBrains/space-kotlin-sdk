package space.jetbrains.api.runtime

class SpaceHttpClientCallContext(
    val server: ServerLocation,
    val tokenSource: TokenSource
) {
    constructor(
        serverUrl: String,
        tokenSource: TokenSource
    ) : this(SpaceServerLocation(serverUrl), tokenSource)
}

class SpaceHttpClientWithCallContext(val client: SpaceHttpClient, val callContext: SpaceHttpClientCallContext)

fun SpaceHttpClient.withCallContext(callContext: SpaceHttpClientCallContext): SpaceHttpClientWithCallContext =
    SpaceHttpClientWithCallContext(this, callContext)

fun SpaceHttpClient.withCallContext(server: ServerLocation, tokenSource: TokenSource): SpaceHttpClientWithCallContext =
    withCallContext(SpaceHttpClientCallContext(server, tokenSource))
