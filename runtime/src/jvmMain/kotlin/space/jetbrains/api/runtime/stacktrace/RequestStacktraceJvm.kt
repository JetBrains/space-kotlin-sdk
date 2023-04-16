package space.jetbrains.api.runtime.stacktrace

public actual fun rethrow(cause: Exception, stackTraceInfo: StackTraceInfo): Nothing {
    val boundaryStackTraceElement = StackTraceElement(BOUNDARY_STACK_TRACE_ELEMENT_CLASS_NAME, "rethrow", null, -1)
    cause.stackTrace = cause.stackTrace + boundaryStackTraceElement + stackTraceInfo.stackTrace
    throw cause
}

public const val BOUNDARY_STACK_TRACE_ELEMENT_CLASS_NAME: String = "COROUTINE_STACKTRACE_PRESERVATION_BOUNDARY"
