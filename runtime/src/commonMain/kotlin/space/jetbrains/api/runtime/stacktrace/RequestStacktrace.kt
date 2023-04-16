package space.jetbrains.api.runtime.stacktrace

/**
 * Stacktrace is generally lost when kotlin coroutine suspends. With [withPreservedStacktrace] stacktrace is preserved like this:
 * - a stack trace is recorded before the [block] is executed
 * - [block] is invoked
 * - if the [block] throws an exception:
 *     - the exception is caught
 *     - a new exception is thrown with the saved stacktrace and the caught exception as a cause
 */
public suspend fun <T> withPreservedStacktrace(message: String, block: suspend () -> T): T {
    // record stacktrace
    val stacktraceInfo = StackTraceInfo(message)
    try {
        return block()
    } catch (e: Exception) {
        rethrow(e, stacktraceInfo)
    }
}

public class StackTraceInfo(message: String) : Exception(message)

public expect fun rethrow(cause: Exception, stackTraceInfo: StackTraceInfo): Nothing
