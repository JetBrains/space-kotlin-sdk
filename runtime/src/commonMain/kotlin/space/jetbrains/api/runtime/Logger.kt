package space.jetbrains.api.runtime

public expect interface Logger {
    public fun trace(message: String)
    public fun warn(message: String)
    public fun error(message: String)
}

internal expect fun getLogger(name: String): Logger
