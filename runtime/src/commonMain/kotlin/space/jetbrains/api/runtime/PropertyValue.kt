package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PropertyValue.None
import space.jetbrains.api.runtime.PropertyValue.Value
import kotlin.reflect.*

public sealed class PropertyValue<out T> {
    public data class Value<out T>(val value: T) : PropertyValue<T>()

    public class None(public val link: ReferenceChainLink) : PropertyValue<Nothing>()
}

public operator fun <T> PropertyValue<T>.getValue(instance: Any?, prop: KProperty<*>): T {
    return when (this) {
        is None -> {
            error("Property '${prop.name}' was not requested. Reference chain:\n${link.referenceChain()}")
        }
        is Value -> value
    }
}
