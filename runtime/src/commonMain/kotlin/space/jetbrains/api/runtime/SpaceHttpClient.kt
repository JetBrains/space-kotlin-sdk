package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.ErrorCodes.AUTHENTICATION_REQUIRED
import space.jetbrains.api.runtime.ErrorCodes.DUPLICATED_ENTITY
import space.jetbrains.api.runtime.ErrorCodes.INTERNAL_SERVER_ERROR
import space.jetbrains.api.runtime.ErrorCodes.NOT_FOUND
import space.jetbrains.api.runtime.ErrorCodes.PAYLOAD_TOO_LARGE
import space.jetbrains.api.runtime.ErrorCodes.PERMISSION_DENIED
import space.jetbrains.api.runtime.ErrorCodes.RATE_LIMITED
import space.jetbrains.api.runtime.ErrorCodes.REQUEST_ERROR
import space.jetbrains.api.runtime.ErrorCodes.VALIDATION_ERROR
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.PayloadTooLarge
import io.ktor.http.HttpStatusCode.Companion.TooManyRequests
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.http.content.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.errors.*

open class RequestException(message: String?, val response: HttpResponse) : Exception(message)

class ValidationException(message: String?, response: HttpResponse) : RequestException(message, response)
class AuthenticationRequiredException(message: String?, response: HttpResponse) : RequestException(message, response)
class PermissionDeniedException(message: String?, response: HttpResponse) : RequestException(message, response)
class NotFoundException(message: String?, response: HttpResponse) : RequestException(message, response)
class DuplicatedEntityException(message: String?, response: HttpResponse) : RequestException(message, response)
class RateLimitedException(message: String?, response: HttpResponse) : RequestException(message, response)
class PayloadTooLargeException(message: String?, response: HttpResponse) : RequestException(message, response)
class InternalServerErrorException(message: String?, response: HttpResponse) : RequestException(message, response)

class SpaceHttpClient(client: HttpClient) {
    private val client = client.config {
        expectSuccess = false
    }

    internal suspend fun auth(url: String, methodBody: Parameters, authHeaderValue: String): ExpiringToken {
        val httpMethod = HttpMethod.Post
        val response = client.request<HttpResponse>(url) {
            method = httpMethod
            accept(ContentType.Application.Json)

            header(HttpHeaders.Authorization, authHeaderValue)

            methodBody.takeIf { !it.isEmpty() }?.let {
                body = TextContent(it.formUrlEncode(), ContentType.Application.FormUrlEncoded)
            }
        }
        val responseTime = now

        val tokenJson = response.readText(Charsets.UTF_8).let(::parseJson)
        handleErrors(response, tokenJson, httpMethod, url)

        val deserialization = DeserializationContext(tokenJson, ReferenceChainLink("auth"))
        return ExpiringToken(
            accessToken = deserialization.child("access_token").let {
                it.requireJson().asString(it.link)
            },
            expires = responseTime.plusSeconds(deserialization.child("expires_in").let {
                it.requireJson().asNumber(it.link)
            }.toLong())
        )
    }

    suspend fun call(
        functionName: String,
        context: SpaceHttpClientCallContext,
        callMethod: HttpMethod,
        path: String,
        partial: PartialBuilder?,
        parameters: Parameters = Parameters.Empty,
        requestBody: JsonValue? = null
    ): DeserializationContext {
        val token = context.tokenSource.token()

        val response = client.request<HttpResponse> {
            url {
                takeFrom(context.server.apiBaseUrl.removeSuffix("/") + "/" + path.removePrefix("/"))

                this.parameters.appendAll(parameters)
                if (partial != null) {
                    this.parameters.append("\$fields", partial.buildQuery())
                }
            }

            method = callMethod
            accept(ContentType.Application.Json)

            token.accessToken.takeIf { it.isNotEmpty() }?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }

            requestBody?.let {
                body = TextContent(it.print(), ContentType.Application.Json)
            }
        }

        val content = response.readText(Charsets.UTF_8).let(::parseJson)
        handleErrors(response, content, callMethod, path)
        return DeserializationContext(content, ReferenceChainLink(functionName))
    }

    private fun handleErrors(response: HttpResponse, responseContent: JsonValue?, callMethod: HttpMethod, path: String) {
        if (!response.status.isSuccess()) {
            val errorDescription = responseContent?.getField("error_description")?.asStringOrNull()
            throw when (responseContent?.getField("error")?.asStringOrNull()) {
                VALIDATION_ERROR -> ValidationException(errorDescription, response)
                AUTHENTICATION_REQUIRED -> AuthenticationRequiredException(errorDescription, response)
                PERMISSION_DENIED -> PermissionDeniedException(errorDescription, response)
                DUPLICATED_ENTITY -> DuplicatedEntityException(errorDescription, response)
                REQUEST_ERROR -> RequestException(errorDescription, response)
                NOT_FOUND -> NotFoundException(errorDescription, response)
                RATE_LIMITED -> RateLimitedException(errorDescription, response)
                PAYLOAD_TOO_LARGE -> PayloadTooLargeException(errorDescription, response)
                INTERNAL_SERVER_ERROR -> InternalServerErrorException(errorDescription, response)
                else -> when (response.status) {
                    BadRequest -> RequestException(BadRequest.description, response)
                    Unauthorized -> AuthenticationRequiredException(BadRequest.description, response)
                    Forbidden -> PermissionDeniedException(Forbidden.description, response)
                    NotFound -> NotFoundException(NotFound.description, response)
                    TooManyRequests -> RateLimitedException(TooManyRequests.description, response)
                    PayloadTooLarge -> PayloadTooLargeException(PayloadTooLarge.description, response)
                    InternalServerError -> InternalServerErrorException(InternalServerError.description, response)
                    else -> IOException("${callMethod.value} request to $path failed")
                }
            }
        }
    }
}
