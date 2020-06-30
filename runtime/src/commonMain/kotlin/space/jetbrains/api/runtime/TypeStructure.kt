package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.Type.*
import space.jetbrains.api.runtime.Type.PrimitiveType.*
import kotlin.js.*
import kotlin.properties.*
import kotlin.reflect.*

abstract class TypeStructure<D : Any> {
    abstract fun deserialize(context: DeserializationContext): D
    abstract fun serialize(value: D): JsonValue

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
    protected fun date(): PropertyProvider<SDate> = property(DateType)
    @JsName("datetime_property")
    protected fun datetime(): PropertyProvider<SDateTime> = property(DateTimeType)
    @JsName("string_property")
    protected fun string(): PropertyProvider<String> = property(StringType)

    @JsName("nullable_property")
    protected fun <T : Any> PropertyProvider<T>.nullable(): PropertyProvider<T?> = property(Nullable(type))
    @JsName("optional_property")
    protected fun <T> PropertyProvider<T>.optional(): PropertyProvider<Option<T>> = property(Optional(type))
    @JsName("list_property")
    protected fun <T> list(prop: PropertyProvider<T>): PropertyProvider<List<T>> = property(ArrayType(prop.type))
    @JsName("map_property")
    protected fun <K, V> map(keyProp: PropertyProvider<K>, valueProp: PropertyProvider<V>): PropertyProvider<Map<K, V>> {
        return property(MapType(keyProp.type, valueProp.type))
    }

    @JsName("obj_property")
    protected fun <T : Any> obj(structure: TypeStructure<T>): PropertyProvider<T> {
        return property(ObjectType(structure))
    }

    @JsName("enum_property")
    protected inline fun <reified T : Enum<T>> enum(): PropertyProvider<T> = enum(enumValues<T>().asList())
    @JsName("enum_property_raw")
    protected fun <T : Enum<T>> enum(values: List<T>): PropertyProvider<T> = property(EnumType(values))

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

    class Property<T>(val name: String, val type: Type<T>)

    class PropertyProvider<T> internal constructor(
        internal val type: Type<T>,
        private val register: (Property<T>) -> Unit
    ) {
        operator fun provideDelegate(thisRef: TypeStructure<*>, property: KProperty<*>): ReadOnlyProperty<TypeStructure<*>, Property<T>> {
            val prop = Property(property.name, type)
            register(prop)
            return object : ReadOnlyProperty<TypeStructure<*>, Property<T>> {
                override fun getValue(thisRef: TypeStructure<*>, property: KProperty<*>): Property<T> = prop
            }
        }
    }
}
