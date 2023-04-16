package space.jetbrains.api.runtime.stacktrace

public actual fun rethrow(cause: Exception, stackTraceInfo: StackTraceInfo): Nothing {
    throw cause
}
