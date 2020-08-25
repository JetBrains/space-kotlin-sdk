package space.jetbrains.api.runtime

public object ErrorCodes {
    public const val VALIDATION_ERROR: String = "validation-error"
    public const val AUTHENTICATION_REQUIRED: String = "authentication-required"
    public const val PERMISSION_DENIED: String = "permission-denied"
    public const val DUPLICATED_ENTITY: String = "duplicated-entity"
    public const val REQUEST_ERROR: String = "request-error"
    public const val NOT_FOUND: String = "not-found"
    public const val RATE_LIMITED: String = "rate-limited"
    public const val PAYLOAD_TOO_LARGE: String = "payload-too-large"
    public const val INTERNAL_SERVER_ERROR: String = "internal-server-error"
}
