package space.jetbrains.api.runtime

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import space.jetbrains.api.runtime.Type.*
import space.jetbrains.api.runtime.Type.PrimitiveType.*
import kotlin.js.*
import kotlin.properties.*
import kotlin.reflect.*

public abstract class TypeStructure<D : Any> {
    public abstract fun deserialize(context: DeserializationContext): D
    public abstract fun serialize(value: D): JsonValue

    private val properties = mutableMapOf<String, Property<*>>()

    protected fun <T> Property<T>.deserialize(context: DeserializationContext): PropertyValue<T> {
        val childContext = context.child(name)
        // TODO track which fields are requested and which are not
        return childContext.json?.let { PropertyValue.Value(type.deserialize(childContext)) }
            ?: PropertyValue.None(childContext.link)
    }

    protected fun <T> Property<T>.serialize(value: T): Pair<String, JsonValue>? {
        return type.serialize(value)?.let { name to it }
    }

    @JsName("byte_property")
    protected fun byte(): PropertyProvider<Byte> = property(NumberType.ByteType)
    @JsName("short_property")
    protected fun short(): PropertyProvider<Short> = property(NumberType.ShortType)
    @JsName("int_property")
    protected fun int(): PropertyProvider<Int> = property(NumberType.IntType)
    @JsName("long_property")
    protected fun long(): PropertyProvider<Long> = property(NumberType.LongType)
    @JsName("float_property")
    protected fun float(): PropertyProvider<Float> = property(NumberType.FloatType)
    @JsName("double_property")
    protected fun double(): PropertyProvider<Double> = property(NumberType.DoubleType)
    @JsName("boolean_property")
    protected fun boolean(): PropertyProvider<Boolean> = property(BooleanType)
    @JsName("date_property")
    protected fun date(): PropertyProvider<LocalDate> = property(DateType)
    @JsName("datetime_property")
    protected fun datetime(): PropertyProvider<Instant> = property(DateTimeType)
    @JsName("string_property")
    protected fun string(): PropertyProvider<String> = property(StringType)

    @JsName("nullable_property")
    protected fun <T : Any> PropertyProvider<T>.nullable(): PropertyProvider<T?> = property(Nullable(type))
    @JsName("optional_property")
    protected fun <T> PropertyProvider<T>.optional(): PropertyProvider<Option<T>> = property(Optional(type))
    @JsName("list_property")
    protected fun <T> list(prop: PropertyProvider<T>): PropertyProvider<List<T>> = property(ArrayType(prop.type))
    @JsName("map_property")
    protected fun <V> map(valueProp: PropertyProvider<V>): PropertyProvider<Map<String, V>> {
        return property(MapType(valueProp.type))
    }

    @JsName("obj_property")
    protected fun <T : Any> obj(structure: TypeStructure<T>): PropertyProvider<T> {
        return property(ObjectType(structure))
    }

    @JsName("enum_property")
    protected inline fun <reified T : Enum<T>> enum(): PropertyProvider<T> = property(EnumType())

    @JsName("property_provider")
    protected fun <T> property(type: Type<T>): PropertyProvider<T> = PropertyProvider(type) {
        properties[it.name] = it
    }

    @JsName("with_className")
    protected fun JsonValue.withClassName(className: String): JsonValue = also {
        if (it.getField("className") == null) {
            it["className"] = jsonString(className)
        }
    }

    public class Property<T>(public val name: String, public val type: Type<T>)

    public class PropertyProvider<T> internal constructor(
        internal val type: Type<T>,
        private val register: (Property<T>) -> Unit
    ) : PropertyDelegateProvider<TypeStructure<*>, ReadOnlyProperty<TypeStructure<*>, Property<T>>> {
        public override operator fun provideDelegate(
            thisRef: TypeStructure<*>,
            property: KProperty<*>,
        ): ReadOnlyProperty<TypeStructure<*>, Property<T>> {
            val prop = Property(property.name, type)
            register(prop)
            return ReadOnlyProperty { _, _ -> prop }
        }
    }
}
