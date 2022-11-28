package space.jetbrains.api.runtime

import io.ktor.client.statement.*

public open class RequestException(message: String?, public val response: HttpResponse, functionName: String) : Exception("$message (calling $functionName)")
public class ValidationException(message: String?, response: HttpResponse, functionName: String) : RequestException(message, response, functionName)
public class AuthenticationRequiredException(message: String?, response: HttpResponse, functionName: String) : RequestException(message, response, functionName)
public class PermissionDeniedException(message: String?, response: HttpResponse, functionName: String) : RequestException(message, response, functionName)
public class NotFoundException(message: String?, response: HttpResponse, functionName: String) : RequestException(message, response, functionName)
public class DuplicatedEntityException(message: String?, response: HttpResponse, functionName: String) : RequestException(message, response, functionName)
public class RateLimitedException(message: String?, response: HttpResponse, functionName: String) : RequestException(message, response, functionName)
public class PayloadTooLargeException(message: String?, response: HttpResponse, functionName: String) : RequestException(message, response, functionName)
public class InternalServerErrorException(message: String?, response: HttpResponse, functionName: String) : RequestException(message, response, functionName)

public class RefreshTokenRevokedException(message: String?, response: HttpResponse, functionName: String) :
    RequestException(message, response, functionName)