package space.jetbrains.api.runtime

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.js.*
import kotlin.properties.*
import kotlin.reflect.*

public abstract class TypeStructure<D : Any>(private val isRecord: Boolean) {
    public abstract fun deserialize(context: DeserializationContext): D
    public abstract fun serialize(value: D): JsonValue
    public open val childClassNames: Set<String> = emptySet()

    protected fun <T> Property<T>.deserialize(context: DeserializationContext): PropertyValue<T> {
        val childContext = context.child(name)

        val shouldBeIncluded = when (inclusionStrategy) {
            InclusionStrategy.ONLY_EXPLICIT -> childContext.partial != null
            InclusionStrategy.ONLY_EXPLICIT_OR_PARENT_EXPLICIT -> childContext.partial != null ||
                context.partial != null && context.partial.hasAllDefault
            InclusionStrategy.PARENT -> childContext.partial != null ||
                context.partial != null && context.partial.hasAllDefault
        }

        return childContext.json?.let { PropertyValue.Value(type.deserialize(childContext)) }
            ?: context.inaccessibleFieldErrorMessagesByFieldName[name]?.let {
                PropertyValue.ValueInaccessible(context.link, it)
            }
            ?: PropertyValue.None(childContext.link, returnNull = type is Type.Nullable<*> && shouldBeIncluded)

    }

    protected fun <T> Property<T>.serialize(value: T): Pair<String, JsonValue>? {
        return type.serialize(value)?.let { name to it }
    }

    @JsName("byte_property")
    protected fun byte(isExtension: Boolean = false): PropertyProvider<Byte> =
        property(Type.NumberType.ByteType, isExtension)

    @JsName("short_property")
    protected fun short(isExtension: Boolean = false): PropertyProvider<Short> =
        property(Type.NumberType.ShortType, isExtension)

    @JsName("int_property")
    protected fun int(isExtension: Boolean = false): PropertyProvider<Int> =
        property(Type.NumberType.IntType, isExtension)

    @JsName("long_property")
    protected fun long(isExtension: Boolean = false): PropertyProvider<Long> =
        property(Type.NumberType.LongType, isExtension)

    @JsName("float_property")
    protected fun float(isExtension: Boolean = false): PropertyProvider<Float> =
        property(Type.NumberType.FloatType, isExtension)

    @JsName("double_property")
    protected fun double(isExtension: Boolean = false): PropertyProvider<Double> =
        property(Type.NumberType.DoubleType, isExtension)

    @JsName("boolean_property")
    protected fun boolean(isExtension: Boolean = false): PropertyProvider<Boolean> =
        property(Type.PrimitiveType.BooleanType, isExtension)

    @JsName("date_property")
    protected fun date(isExtension: Boolean = false): PropertyProvider<LocalDate> =
        property(Type.PrimitiveType.DateType, isExtension)

    @JsName("datetime_property")
    protected fun datetime(isExtension: Boolean = false): PropertyProvider<Instant> =
        property(Type.PrimitiveType.DateTimeType, isExtension)

    @JsName("string_property")
    protected fun string(isExtension: Boolean = false): PropertyProvider<String> =
        property(Type.PrimitiveType.StringType, isExtension)

    @JsName("nullable_property")
    protected fun <T : Any> PropertyProvider<T>.nullable(): PropertyProvider<T?> =
        property(Type.Nullable(type), isExtension)

    @JsName("optional_property")
    protected fun <T> PropertyProvider<T>.optional(): PropertyProvider<Option<T>> =
        property(Type.Optional(type), isExtension)

    @JsName("list_property")
    protected fun <T> list(prop: PropertyProvider<T>): PropertyProvider<List<T>> =
        property(Type.ArrayType(prop.type), prop.isExtension)

    @JsName("map_property")
    protected fun <V> map(valueProp: PropertyProvider<V>): PropertyProvider<Map<String, V>> {
        return property(Type.MapType(valueProp.type), valueProp.isExtension)
    }

    @JsName("obj_property")
    protected fun <T : Any> obj(structure: TypeStructure<T>, isExtension: Boolean = false): PropertyProvider<T> {
        return property(Type.ObjectType(structure), isExtension)
    }

    @JsName("enum_property")
    protected inline fun <reified T : Enum<T>> enum(isExtension: Boolean = false): PropertyProvider<T> =
        property(Type.EnumType(), isExtension)

    @JsName("property_provider")
    protected fun <T> property(type: Type<T>, isExtension: Boolean = false): PropertyProvider<T> =
        PropertyProvider(type, isExtension, isRecord)

    @JsName("with_className")
    protected fun JsonValue.withClassName(className: String): JsonValue = also {
        if (it.getField("className") == null) {
            it["className"] = jsonString(className)
        }
    }

    protected fun minorDeserializationError(message: String, link: ReferenceChainLink): Nothing =
        throw DeserializationException.Minor(message, link)

    public class Property<T> internal constructor(
        public val name: String,
        public val type: Type<T>,
        public val inclusionStrategy: InclusionStrategy,
    )

    public enum class InclusionStrategy {
        ONLY_EXPLICIT,
        ONLY_EXPLICIT_OR_PARENT_EXPLICIT,
        PARENT
    }

    public class PropertyProvider<T> internal constructor(
        internal val type: Type<T>,
        internal val isExtension: Boolean,
        private val isOuterRecord: Boolean,
    ) : PropertyDelegateProvider<TypeStructure<*>, ReadOnlyProperty<TypeStructure<*>, Property<T>>> {
        public override operator fun provideDelegate(
            thisRef: TypeStructure<*>,
            property: KProperty<*>,
        ): ReadOnlyProperty<TypeStructure<*>, Property<T>> {
            val inclusionStrategy = when {
                isExtension -> InclusionStrategy.ONLY_EXPLICIT
                isOuterRecord && property.name == "id" -> InclusionStrategy.ONLY_EXPLICIT_OR_PARENT_EXPLICIT
                else -> InclusionStrategy.PARENT
            }
            val prop = Property(property.name, type, inclusionStrategy)
            return ReadOnlyProperty { _, _ -> prop }
        }
    }
}
