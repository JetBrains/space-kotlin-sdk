package space.jetbrains.api.runtime

import org.slf4j.LoggerFactory

public actual typealias Logger = org.slf4j.Logger

internal actual fun getLogger(name: String): Logger {
    return LoggerFactory.getLogger(name)!!
}
