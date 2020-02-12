package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PropertyValue.None
import space.jetbrains.api.runtime.PropertyValue.Value
import kotlin.reflect.*

sealed class PropertyValue<out T> {
    data class Value<out T>(val value: T) : PropertyValue<T>()

    class None(val link: ReferenceChainLink) : PropertyValue<Nothing>()
}

operator fun <T> PropertyValue<T>.getValue(instance: Any?, prop: KProperty<*>): T {
    return when (this) {
        is None -> {
            error("Property '${prop.name}' was not requested. Reference chain:\n${link.referenceChain()}")
        }
        is Value -> value
    }
}
