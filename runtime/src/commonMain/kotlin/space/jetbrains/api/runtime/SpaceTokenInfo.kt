package space.jetbrains.api.runtime

import kotlinx.datetime.Instant

public data class SpaceTokenInfo(
    public val accessToken: String,
    public val expires: Instant?,
    public val refreshToken: String? = null,
)

@Deprecated("Use SpaceTokenInfo", ReplaceWith("SpaceTokenInfo"))
public typealias ExpiringToken = SpaceTokenInfo
