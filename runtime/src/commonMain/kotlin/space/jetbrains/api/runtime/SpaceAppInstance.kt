package space.jetbrains.api.runtime

public class SpaceAppInstance private constructor(
    clientId: String?,
    clientSecret: String?,
    spaceServerUrl: String,
    @Suppress("UNUSED_PARAMETER") unit: Unit, // to prevent JVM declaration clash
) {
    public constructor(
        clientId: String,
        clientSecret: String,
        spaceServerUrl: String
    ) : this(clientId, clientSecret, spaceServerUrl, Unit)

    private val _clientId = clientId
    private val _clientSecret = clientSecret

    public val clientId: String get() = _clientId ?: error(::clientId.name + " is not defined")
    public val clientSecret: String get() = _clientSecret ?: error(::clientSecret.name + " is not defined")
    public val spaceServer: SpaceServerLocation = SpaceServerLocation(spaceServerUrl)

    public companion object {
        /**
         * This function exists for migration purposes.
         * Creation of [SpaceAppInstance] without client credentials will be prohibited in the future.
         */
        @Deprecated("Specify clientId and clientSecret")
        public fun withoutCredentials(spaceServerUrl: String): SpaceAppInstance = SpaceAppInstance(
            clientId = null,
            clientSecret = null,
            spaceServerUrl = spaceServerUrl,
            unit = Unit,
        )
    }
}