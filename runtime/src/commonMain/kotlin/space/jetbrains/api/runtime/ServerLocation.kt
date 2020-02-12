package space.jetbrains.api.runtime

interface ServerLocation {
    val apiUrl: String
}

class SpaceServerLocation(serverUrl: String) : ServerLocation {
    override val apiUrl = "${serverUrl.trimEnd('/')}/api/http/"
}
