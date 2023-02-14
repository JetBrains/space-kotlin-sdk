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
    val stacktraceInfo = SuspendAwareThrowable(message)
    try {
        return block()
    } catch (e: Throwable) {
        stacktraceInfo.rethrow(e)
    }
}

/**
 * Originally taken from here: https://gist.github.com/morj/223e53326a6df994a92c260a474fa0a1
 */
private class SuspendAwareThrowable(message: String) : Throwable(message) {
    override var cause: Throwable? = null

    fun rethrow(cause: Throwable): Nothing {
        this.cause = cause
        throw this
    }
}
