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
    public val clientSecretOrNull: String? = clientSecret

    public val clientId: String get() = _clientId ?: error(::clientId.name + " is not defined")
    public val clientSecret: String get() = clientSecretOrNull ?: error(::clientSecret.name + " is not defined")

    public val spaceServer: SpaceServerLocation = SpaceServerLocation(spaceServerUrl)

    public companion object {
        public fun withoutSecret(clientId: String, spaceServerUrl: String): SpaceAppInstance = SpaceAppInstance(
            clientId = clientId,
            clientSecret = null,
            spaceServerUrl = spaceServerUrl,
            unit = Unit,
        )

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