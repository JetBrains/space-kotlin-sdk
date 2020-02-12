package space.jetbrains.api.runtime

class SpaceHttpClientCallContext(
    val server: ServerLocation,
    val tokenSource: SimpleTokenSource
) {
    constructor(
        serverUrl: String,
        tokenSource: SimpleTokenSource
    ) : this(SpaceServerLocation(serverUrl), tokenSource)
}

class SpaceHttpClientWithCallContext(val client: SpaceHttpClient, val callContext: SpaceHttpClientCallContext)

fun SpaceHttpClient.withCallContext(callContext: SpaceHttpClientCallContext) =
    SpaceHttpClientWithCallContext(this, callContext)
