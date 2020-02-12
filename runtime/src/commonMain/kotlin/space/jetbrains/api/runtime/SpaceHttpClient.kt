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

    suspend fun call(
        functionName: String,
        context: SpaceHttpClientCallContext,
        callMethod: HttpMethod,
        path: String,
        partial: Partial<*>?,
        parameters: List<Pair<String, String>> = emptyList(),
        requestBody: JsonValue? = null
    ): DeserializationContext<*> {
        val token = context.tokenSource.token()
        val url = URLBuilder(context.server.apiUrl + path).apply {
            parameters.forEach {
                this.parameters.append(it.first, it.second)
            }
            partial?.let { this.parameters.append("\$fields", it.buildQuery()) }
        }.build()

        return client.request<HttpResponse>(url) {
            method = callMethod
            accept(ContentType.Application.Json)

            token.accessToken.takeIf { it.isNotEmpty() }?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }

            requestBody?.let {
                body = TextContent(it.print(), ContentType.Application.Json)
            }
        }.let {
            val content = it.readText(Charsets.UTF_8).let(::parseJson)
            if (!it.status.isSuccess()) {
                val errorDescription = content?.get("error_description")?.asStringOrNull()
                throw when (content?.get("error")?.asStringOrNull()) {
                    VALIDATION_ERROR -> ValidationException(errorDescription, it)
                    AUTHENTICATION_REQUIRED -> AuthenticationRequiredException(errorDescription, it)
                    PERMISSION_DENIED -> PermissionDeniedException(errorDescription, it)
                    DUPLICATED_ENTITY -> DuplicatedEntityException(errorDescription, it)
                    REQUEST_ERROR -> RequestException(errorDescription, it)
                    NOT_FOUND -> NotFoundException(errorDescription, it)
                    RATE_LIMITED -> RateLimitedException(errorDescription, it)
                    PAYLOAD_TOO_LARGE -> PayloadTooLargeException(errorDescription, it)
                    INTERNAL_SERVER_ERROR -> InternalServerErrorException(errorDescription, it)
                    else -> when (it.status) {
                        BadRequest -> RequestException(BadRequest.description, it)
                        Unauthorized -> AuthenticationRequiredException(BadRequest.description, it)
                        Forbidden -> PermissionDeniedException(Forbidden.description, it)
                        NotFound -> NotFoundException(NotFound.description, it)
                        TooManyRequests -> RateLimitedException(TooManyRequests.description, it)
                        PayloadTooLarge -> PayloadTooLargeException(PayloadTooLarge.description, it)
                        InternalServerError -> InternalServerErrorException(InternalServerError.description, it)
                        else -> IOException("${callMethod.value} request to $path failed")
                    }
                }
            }
            DeserializationContext(content, partial, ReferenceChainLink(functionName))
        }
    }
}
