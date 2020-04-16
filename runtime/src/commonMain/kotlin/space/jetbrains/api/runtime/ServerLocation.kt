package space.jetbrains.api.runtime

interface ServerLocation {
    val oauthUrl: String
    val apiUrl: String
}

class SpaceServerLocation(serverUrl: String) : ServerLocation {
    override val oauthUrl = "${serverUrl.trimEnd('/')}/oauth/token"
    override val apiUrl = "${serverUrl.trimEnd('/')}/api/http/"
}
