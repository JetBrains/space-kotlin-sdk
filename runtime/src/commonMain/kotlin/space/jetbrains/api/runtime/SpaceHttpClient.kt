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
import mu.KotlinLogging
import space.jetbrains.api.runtime.epoch.EpochTrackingPlugin

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

private val log = KotlinLogging.logger {}

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
    requestBody = requestBody
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
    requestBody: JsonValue? = null,
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
            setBody(TextContent(it.print(), ContentType.Application.Json))
        }

        requestHeaders?.forEach {
            headers.append(it.first, it.second)
        }
    }

    while (true) {
        val request = HttpRequestBuilder().takeFrom(templateRequest)
        auth.token(ktorClient, appInstance).accessToken.takeIf { it.isNotEmpty() }?.let {
            request.header(HttpHeaders.Authorization, "Bearer $it")
        }
        val response = ktorClient.request(request)
        val responseText = response.bodyAsText()
        log.trace { "Response for ${request.method.value} request to ${request.url.buildString()}:\n$responseText" }
        val content = responseText.let(::parseJson)
        if (!throwErrorOrReturnWhetherToRetry(response, content, callMethod, path)) {
            return DeserializationContext(content, ReferenceChainLink(functionName), partial)
        }
    }
}

internal suspend fun auth(
    ktorClient: HttpClient,
    url: String,
    methodBody: Parameters,
    authHeaderValue: String
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

    val tokenJson = response.bodyAsText().let(::parseJson)
    throwErrorOrReturnWhetherToRetry(response, tokenJson, httpMethod, url)

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
    callMethod: HttpMethod,
    path: String
): Boolean {
    if (!response.status.isSuccess()) {
        val errorDescription = responseContent?.getField("error_description")?.asStringOrNull()
        throw when (responseContent?.getField("error")?.asStringOrNull()) {
            ErrorCodes.VALIDATION_ERROR -> ValidationException(errorDescription, response)
            ErrorCodes.AUTHENTICATION_REQUIRED -> when (errorDescription) {
                "Access token has expired" -> return true
                "Refresh token associated with the access token is revoked" ->
                    RefreshTokenRevokedException(errorDescription, response)
                else -> AuthenticationRequiredException(errorDescription, response)
            }
            ErrorCodes.PERMISSION_DENIED -> PermissionDeniedException(errorDescription, response)
            ErrorCodes.DUPLICATED_ENTITY -> DuplicatedEntityException(errorDescription, response)
            ErrorCodes.REQUEST_ERROR -> RequestException(errorDescription, response)
            ErrorCodes.NOT_FOUND -> NotFoundException(errorDescription, response)
            ErrorCodes.RATE_LIMITED -> RateLimitedException(errorDescription, response)
            ErrorCodes.PAYLOAD_TOO_LARGE -> PayloadTooLargeException(errorDescription, response)
            ErrorCodes.INTERNAL_SERVER_ERROR -> InternalServerErrorException(errorDescription, response)
            else -> when (response.status) {
                HttpStatusCode.BadRequest -> RequestException(HttpStatusCode.BadRequest.description, response)
                HttpStatusCode.Unauthorized -> AuthenticationRequiredException(
                    HttpStatusCode.BadRequest.description,
                    response
                )
                HttpStatusCode.Forbidden -> PermissionDeniedException(HttpStatusCode.Forbidden.description, response)
                HttpStatusCode.NotFound -> NotFoundException(HttpStatusCode.NotFound.description, response)
                HttpStatusCode.TooManyRequests -> RateLimitedException(
                    HttpStatusCode.TooManyRequests.description,
                    response
                )
                HttpStatusCode.PayloadTooLarge -> PayloadTooLargeException(
                    HttpStatusCode.PayloadTooLarge.description,
                    response
                )
                HttpStatusCode.InternalServerError -> InternalServerErrorException(
                    HttpStatusCode.InternalServerError.description,
                    response
                )
                else -> IOException("${callMethod.value} request to $path failed")
            }
        }
    }
    return false
}