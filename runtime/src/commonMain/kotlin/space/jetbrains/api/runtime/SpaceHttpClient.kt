package space.jetbrains.api.runtime

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.errors.*
import kotlinx.datetime.Clock.System
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import space.jetbrains.api.runtime.epoch.EpochTrackingPlugin
import space.jetbrains.api.runtime.stacktrace.withPreservedStacktrace

public fun HttpClientConfig<*>.configureKtorClientForSpace() {
    expectSuccess = false
    install(EpochTrackingPlugin)
}

public fun ktorClientForSpace(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient = HttpClient {
    block()
    configureKtorClientForSpace()
}

public fun ktorClientForSpace(
    engine: HttpClientEngine,
    block: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(engine) {
    block()
    configureKtorClientForSpace()
}

public fun <T : HttpClientEngineConfig> ktorClientForSpace(
    engineFactory: HttpClientEngineFactory<T>,
    block: HttpClientConfig<T>.() -> Unit = {},
): HttpClient = HttpClient(engineFactory) {
    block()
    configureKtorClientForSpace()
}

@Deprecated(
    "Use HttpClient from ktorClientForSpace() or configured with configureKtorClientForSpace()",
    ReplaceWith("HttpClient", "io.ktor.client.HttpClient")
)
public typealias SpaceHttpClient = HttpClient

@Deprecated(
    "Use HttpClient from ktorClientForSpace() or configured with configureKtorClientForSpace()",
    ReplaceWith("ktorClient.config { configureKtorClientForSpace() }")
)
@Suppress("FunctionName")
public fun SpaceHttpClient(ktorClient: HttpClient): HttpClient = ktorClient.config { configureKtorClientForSpace() }

private val log = getLogger("space.jetbrains.api.runtime.SpaceHttpClient")

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Use SpaceClient to call Space API")
public suspend fun HttpClient.call(
    functionName: String,
    appInstance: SpaceAppInstance,
    auth: SpaceAuth,
    callMethod: HttpMethod,
    path: String,
    partial: PartialBuilder.Explicit?,
    parameters: Parameters = Parameters.Empty,
    requestBody: JsonValue? = null
): DeserializationContext = callSpaceApi(
    ktorClient = this,
    functionName = functionName,
    appInstance = appInstance,
    auth = auth,
    callMethod = callMethod,
    path = path,
    partial = partial,
    parameters = parameters,
    requestBody = requestBody?.let { TextContent(it.print(), ContentType.Application.Json) }
)

internal suspend fun callSpaceApi(
    ktorClient: HttpClient,
    functionName: String,
    appInstance: SpaceAppInstance,
    auth: SpaceAuth,
    callMethod: HttpMethod,
    path: String,
    partial: PartialBuilder.Explicit?,
    parameters: Parameters = Parameters.Empty,
    requestBody: OutgoingContent? = null,
    requestHeaders: List<Pair<String, String>>? = null,
): DeserializationContext {
    val templateRequest = HttpRequestBuilder().apply {
        url {
            takeFrom(appInstance.spaceServer.apiBaseUrl.removeSuffix("/") + "/" + path.removePrefix("/"))

            this.parameters.appendAll(parameters)
            if (partial != null) {
                this.parameters.append("\$fields", partial.buildQuery())
            }
        }

        method = callMethod
        accept(ContentType.Application.Json)


        requestBody?.let {
            val bodyText = (it as? TextContent)?.text
            log.trace("Request to Space: $functionName (${callMethod.value} request to ${url.buildString()}):\n$bodyText")
            setBody(it)
        }

        requestHeaders?.forEach {
            headers.append(it.first, it.second)
        }
    }

    while (true) {
        val request = HttpRequestBuilder().takeFrom(templateRequest)

        val isAccessTokenRefreshable = withPreservedStacktrace("exception while getting access token for Space API call: $functionName") {
            val spaceTokenInfo = auth.token(ktorClient, appInstance)
            spaceTokenInfo.accessToken.takeIf { it.isNotEmpty() }?.let {
                request.header(HttpHeaders.Authorization, "Bearer $it")
            }
            spaceTokenInfo.expires != null
        }

        val (retry, content) = withPreservedStacktrace("exception during Space API call: $functionName") {
            val response = ktorClient.request(request)
            val responseText = response.bodyAsText()
            log.trace("Response from Space for $functionName (${request.method.value} request to ${request.url.buildString()}):\n$responseText")
            val content = responseText.let(::parseJson)
            throwErrorOrReturnWhetherToRetry(response, content, functionName, isAccessTokenRefreshable) to content
        }

        if (!retry) {
            return DeserializationContext(content, ReferenceChainLink(functionName), partial)
        }
    }
}

internal suspend fun auth(
    ktorClient: HttpClient,
    url: String,
    methodBody: Parameters,
    authHeaderValue: String?,
): SpaceTokenInfo {
    val httpMethod = HttpMethod.Post
    val response = ktorClient.request(url) {
        this.method = httpMethod
        this.accept(ContentType.Application.Json)

        this.header(HttpHeaders.Authorization, authHeaderValue)

        methodBody.takeIf { !it.isEmpty() }?.let {
            setBody(TextContent(it.formUrlEncode(), ContentType.Application.FormUrlEncoded))
        }
    }
    val responseTime = System.now()

    val tokenJson = response.bodyAsText().let(::tryParseJson)
    throwErrorOrReturnWhetherToRetry(response, tokenJson, url, retryExpiredAccessTokenError = false)

    val deserialization = DeserializationContext(tokenJson, ReferenceChainLink("auth"), null)
    return SpaceTokenInfo(
        accessToken = deserialization.child("access_token").let {
            it.requireJson().asString(it.link)
        },
        expires = responseTime.plus(
            value = deserialization.child("expires_in").let {
                it.requireJson().asNumber(it.link)
            }.toLong(),
            unit = DateTimeUnit.SECOND,
        ),
        refreshToken = deserialization.child("refresh_token").let { it.json?.asString(it.link) },
    )
}

private fun throwErrorOrReturnWhetherToRetry(
    response: HttpResponse,
    responseContent: JsonValue?,
    functionName: String,
    retryExpiredAccessTokenError: Boolean,
): Boolean {
    if (!response.status.isSuccess()) {
        val errorDescription = responseContent?.getFieldOrNull("error_description")?.asStringOrNull()
        throw when (responseContent?.getFieldOrNull("error")?.asStringOrNull()) {
            ErrorCodes.VALIDATION_ERROR -> ValidationException(errorDescription, response, functionName)
            ErrorCodes.AUTHENTICATION_REQUIRED -> when (errorDescription) {
                "Access token has expired" ->
                    if (retryExpiredAccessTokenError) return true
                    else AuthenticationRequiredException(errorDescription, response, functionName)

                "Refresh token associated with the access token is revoked" ->
                    RefreshTokenRevokedException(errorDescription, response, functionName)

                else -> AuthenticationRequiredException(errorDescription, response, functionName)
            }

            ErrorCodes.PERMISSION_DENIED -> PermissionDeniedException(errorDescription, response, functionName)
            ErrorCodes.DUPLICATED_ENTITY -> DuplicatedEntityException(errorDescription, response, functionName)
            ErrorCodes.REQUEST_ERROR -> RequestException(errorDescription, response, functionName)
            ErrorCodes.NOT_FOUND -> NotFoundException(errorDescription, response, functionName)
            ErrorCodes.RATE_LIMITED -> RateLimitedException(errorDescription, response, functionName)
            ErrorCodes.PAYLOAD_TOO_LARGE -> PayloadTooLargeException(errorDescription, response, functionName)
            ErrorCodes.INTERNAL_SERVER_ERROR -> InternalServerErrorException(errorDescription, response, functionName)
            else -> when (response.status) {
                HttpStatusCode.BadRequest -> RequestException(
                    HttpStatusCode.BadRequest.description + errorDescription?.let { ": $it" }.orEmpty(),
                    response,
                    functionName
                )

                HttpStatusCode.Unauthorized -> AuthenticationRequiredException(
                    HttpStatusCode.BadRequest.description + errorDescription?.let { ": $it" }.orEmpty(),
                    response,
                    functionName
                )

                HttpStatusCode.Forbidden -> PermissionDeniedException(
                    HttpStatusCode.Forbidden.description + errorDescription?.let { ": $it" }.orEmpty(),
                    response,
                    functionName
                )

                HttpStatusCode.NotFound -> NotFoundException(
                    HttpStatusCode.NotFound.description + errorDescription?.let { ": $it" }.orEmpty(),
                    response,
                    functionName
                )

                HttpStatusCode.TooManyRequests -> RateLimitedException(
                    HttpStatusCode.TooManyRequests.description + errorDescription?.let { ": $it" }.orEmpty(),
                    response,
                    functionName
                )

                HttpStatusCode.PayloadTooLarge -> PayloadTooLargeException(
                    HttpStatusCode.PayloadTooLarge.description + errorDescription?.let { ": $it" }.orEmpty(),
                    response,
                    functionName
                )

                HttpStatusCode.InternalServerError -> InternalServerErrorException(
                    HttpStatusCode.InternalServerError.description + errorDescription?.let { ": $it" }.orEmpty(),
                    response,
                    functionName
                )

                else -> IOException("${response.request.method.value} request to ${response.request.url.encodedPath} failed (calling $functionName)")
            }
        }
    }
    return false
}