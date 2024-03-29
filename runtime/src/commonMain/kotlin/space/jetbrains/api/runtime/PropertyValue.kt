package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PropertyValue.None
import space.jetbrains.api.runtime.PropertyValue.Value
import space.jetbrains.api.runtime.PropertyValue.ValueInaccessible

public sealed class PropertyValue<out T> {
    public data class Value<out T>(val value: T) : PropertyValue<T>()

    public class ValueInaccessible(
        public val link: ReferenceChainLink,
        internal val message: String
    ) : PropertyValue<Nothing>()

    public class None(public val link: ReferenceChainLink, internal val returnNull: Boolean) : PropertyValue<Nothing>()

    internal companion object {
        val log = getLogger(PropertyValue::class.qualifiedName!!)
    }
}

public class PropertyValueInaccessibleException(message: String) : Exception(message)

public fun <T> PropertyValue<T>.getValue(propName: String): T {
    return when (this) {
        is None -> {
            if (returnNull) {
                PropertyValue.log.warn("Property '${propName}' is not present, using null. Reference chain:\n${link.referenceChain()}")
                @Suppress("UNCHECKED_CAST")
                return null as T
            }
            error("Property '${propName}' was not requested. Reference chain:\n${link.referenceChain()}")
        }
        is ValueInaccessible -> throw PropertyValueInaccessibleException(message)
        is Value -> value
    }
}
