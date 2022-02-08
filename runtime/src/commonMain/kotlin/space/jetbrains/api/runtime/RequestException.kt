package space.jetbrains.api.runtime

import io.ktor.client.statement.HttpResponse

public open class RequestException(message: String?, public val response: HttpResponse) : Exception(message)
public class ValidationException(message: String?, response: HttpResponse) : RequestException(message, response)
public class AuthenticationRequiredException(message: String?, response: HttpResponse) : RequestException(message, response)
public class PermissionDeniedException(message: String?, response: HttpResponse) : RequestException(message, response)
public class NotFoundException(message: String?, response: HttpResponse) : RequestException(message, response)
public class DuplicatedEntityException(message: String?, response: HttpResponse) : RequestException(message, response)
public class RateLimitedException(message: String?, response: HttpResponse) : RequestException(message, response)
public class PayloadTooLargeException(message: String?, response: HttpResponse) : RequestException(message, response)
public class InternalServerErrorException(message: String?, response: HttpResponse) : RequestException(message, response)

public class RefreshTokenRevokedException(message: String?, response: HttpResponse) :
    RequestException(message, response)