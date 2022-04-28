package space.jetbrains.api.runtime

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.utils.io.core.Closeable
import kotlinx.datetime.*
import space.jetbrains.api.runtime.epoch.EpochTrackingFeature
import kotlin.reflect.KFunction1

public class SpaceClient private constructor(
    public val ktorClient: HttpClient,
    public val appInstance: SpaceAppInstance,
    public val auth: SpaceAuth,
    private val manageKtorClient: Boolean,
) : Closeable {
    /**
     * When this constructor is used, the caller is responsible for closing the [ktorClient],
     * closing this [SpaceClient] doesn't do anything.
     * Use it when you want to reuse [HttpClient] for multiple instances of [SpaceClient],
     * for example when you make calls on behalf of different users.
     *
     * [ktorClient] must be either created with [ktorClientForSpace] or configured with [configureKtorClientForSpace].
     *
     * ```kotlin
     * ktorClientForSpace().use { ktorClient ->
     *     users.forEach { user ->
     *         val userStarredProjects = SpaceClient(
     *             ktorClient,
     *             appInstance,
     *             SpaceAuth.RefreshToken(user.getRefreshToken(), scope)
     *         ).projects.getAllProjects(starred = true)
     *         // ...
     *     }
     * }
     * ```
     */
    public constructor(
        ktorClient: HttpClient,
        appInstance: SpaceAppInstance,
        auth: SpaceAuth,
    ) : this(ktorClient, appInstance, auth, manageKtorClient = false)


    /**
     * When this constructor is used, the caller is responsible for closing the [ktorClient],
     * closing this [SpaceClient] doesn't do anything.
     * Use it when you want to reuse [HttpClient] for multiple instances of [SpaceClient],
     * for example when you make calls on behalf of different users.
     *
     * [ktorClient] must be either created with [ktorClientForSpace] or configured with [configureKtorClientForSpace].
     *
     * ```kotlin
     * ktorClientForSpace().use { ktorClient ->
     *     users.forEach { user ->
     *         val userStarredProjects = SpaceClient(
     *             ktorClient,
     *             installedApp,
     *             SpaceAuth.RefreshToken(user.getRefreshToken(), scope)
     *         ).projects.getAllProjects(starred = true)
     *         // ...
     *     }
     * }
     * ```
     */

    public constructor(
        ktorClient: HttpClient,
        serverUrl: String,
        token: String,
    ) : this(
        ktorClient = ktorClient,
        appInstance = @Suppress("DEPRECATION") SpaceAppInstance.withoutCredentials(serverUrl),
        auth = SpaceAuth.Token(token),
        manageKtorClient = false
    )

    /**
     * When this constructor is used, the resulting [SpaceClient] must be closed,
     * in order for the underlying [HttpClient] to be closed too.
     * Use it when your app needs only one instance of [SpaceClient].
     *
     * ```kotlin
     * SpaceClient(appInstance, SpaceAuth.ClientCredentials()).use {
     *     it.chats.messages.sendMessage(...)
     * }
     * ```
     */
    public constructor(
        appInstance: SpaceAppInstance,
        auth: SpaceAuth,
        ktorClientConfig: HttpClientConfig<*>.() -> Unit = {},
    ) : this(ktorClientForSpace(ktorClientConfig), appInstance, auth, manageKtorClient = true)

    /**
     * When this constructor is used, the resulting [SpaceClient] must be closed,
     * in order for the underlying [HttpClient] to be closed too.
     * Use it when your app needs only one instance of [SpaceClient].
     *
     * ```kotlin
     * SpaceClient(spaceServerUrl, token).use {
     *     it.chats.messages.sendMessage(...)
     * }
     * ```
     */
    public constructor(
        serverUrl: String,
        token: String,
        ktorClientConfig: HttpClientConfig<*>.() -> Unit = {},
    ) : this(
        ktorClient = ktorClientForSpace(ktorClientConfig),
        appInstance = @Suppress("DEPRECATION") SpaceAppInstance.withoutCredentials(serverUrl),
        auth = SpaceAuth.Token(token),
        manageKtorClient = true
    )

    init {
        class Hack(val value: Boolean) : Throwable()
        val expectSuccess = try {
            ktorClient.config { throw Hack(expectSuccess) }
            error("Unreachable")
        } catch (e: Hack) { e.value }

        require(!expectSuccess && ktorClient.pluginOrNull(EpochTrackingFeature) != null) {
            val ktorClientForSpace: KFunction1<Nothing, HttpClient> = ::ktorClientForSpace
            "${::ktorClient.name} should be either created with ${ktorClientForSpace.name}() or configured with " +
                "${HttpClientConfig<*>::configureKtorClientForSpace.name}()"
        }
    }

    public val server: SpaceServerLocation get() = appInstance.spaceServer

    public suspend fun token(): SpaceTokenInfo = auth.token(ktorClient, appInstance)

    @Deprecated("Use ktorClient", ReplaceWith("ktorClient"))
    public val client: HttpClient get() = ktorClient

    override fun close() {
        if (manageKtorClient) ktorClient.close()
    }
}

@Deprecated("Create SpaceClient explicitly")
public class SpaceHttpClientCallContext(serverUrl: String, public val tokenSource: SpaceAuth) {
    public val server: SpaceServerLocation = SpaceServerLocation(serverUrl)
}

@Deprecated("Use SpaceClient", ReplaceWith("SpaceClient", "space.jetbrains.api.runtime.SpaceClient"))
public typealias SpaceHttpClientWithCallContext = SpaceClient

@Deprecated(
    message = "Create SpaceClient explicitly",
    replaceWith = ReplaceWith(
        "SpaceClient(this, InstalledApp.withoutCredentials(callContext.server.serverUrl), callContext.tokenSource)",
        "space.jetbrains.api.runtime.SpaceClient"
    ),
)
public fun HttpClient.withCallContext(callContext: @Suppress("DEPRECATION") SpaceHttpClientCallContext): SpaceClient =
    SpaceClient(
        ktorClient = this,
        appInstance = @Suppress("DEPRECATION") SpaceAppInstance.withoutCredentials(callContext.server.serverUrl),
        auth = callContext.tokenSource
    )

@Deprecated(
    "Create SpaceClient explicitly",
    ReplaceWith(
        "SpaceClient(this, InstalledApp.withoutCredentials(serverUrl), tokenSource)",
        "space.jetbrains.api.runtime.SpaceClient"
    ),
)
public fun HttpClient.withCallContext(serverUrl: String, tokenSource: SpaceAuth): SpaceClient =
    SpaceClient(this, @Suppress("DEPRECATION") SpaceAppInstance.withoutCredentials(serverUrl), tokenSource)

internal fun SpaceTokenInfo.expired(gapSeconds: Long = 5): Boolean {
    return expires?.let { Clock.System.now().plus(gapSeconds, DateTimeUnit.SECOND) > it } ?: false
}
