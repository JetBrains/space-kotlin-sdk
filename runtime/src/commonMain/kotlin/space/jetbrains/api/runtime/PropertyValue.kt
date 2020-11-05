package space.jetbrains.api.runtime

import mu.KotlinLogging
import space.jetbrains.api.runtime.PropertyValue.None
import space.jetbrains.api.runtime.PropertyValue.Value
import kotlin.reflect.*

public sealed class PropertyValue<out T> {
    public data class Value<out T>(val value: T) : PropertyValue<T>()

    public class None(public val link: ReferenceChainLink, internal val returnNull: Boolean) : PropertyValue<Nothing>()

    internal companion object {
        val log = KotlinLogging.logger {}
    }
}

public operator fun <T> PropertyValue<T>.getValue(instance: Any?, prop: KProperty<*>): T {
    return when (this) {
        is None -> {
            if (returnNull) {
                PropertyValue.log.warn {
                    "Property '${prop.name}' is not present, using null. Reference chain:\n${link.referenceChain()}"
                }
                @Suppress("UNCHECKED_CAST")
                return null as T
            }
            error("Property '${prop.name}' was not requested. Reference chain:\n${link.referenceChain()}")
        }
        is Value -> value
    }
}
