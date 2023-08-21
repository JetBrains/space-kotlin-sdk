package space.jetbrains.api.runtime

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.time.Duration

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

    protected fun byte(isExtension: Boolean = false): PropertyProvider<Byte> =
        property(Type.NumberType.ByteType, isExtension)

    protected fun short(isExtension: Boolean = false): PropertyProvider<Short> =
        property(Type.NumberType.ShortType, isExtension)

    protected fun int(isExtension: Boolean = false): PropertyProvider<Int> =
        property(Type.NumberType.IntType, isExtension)

    protected fun long(isExtension: Boolean = false): PropertyProvider<Long> =
        property(Type.NumberType.LongType, isExtension)

    protected fun float(isExtension: Boolean = false): PropertyProvider<Float> =
        property(Type.NumberType.FloatType, isExtension)

    protected fun double(isExtension: Boolean = false): PropertyProvider<Double> =
        property(Type.NumberType.DoubleType, isExtension)

    protected fun boolean(isExtension: Boolean = false): PropertyProvider<Boolean> =
        property(Type.PrimitiveType.BooleanType, isExtension)

    protected fun date(isExtension: Boolean = false): PropertyProvider<LocalDate> =
        property(Type.PrimitiveType.DateType, isExtension)

    protected fun datetime(isExtension: Boolean = false): PropertyProvider<Instant> =
        property(Type.PrimitiveType.DateTimeType, isExtension)

    protected fun duration(isExtension: Boolean = false): PropertyProvider<Duration> =
        property(Type.PrimitiveType.DurationType, isExtension)

    protected fun string(isExtension: Boolean = false): PropertyProvider<String> =
        property(Type.PrimitiveType.StringType, isExtension)

    protected fun <T : Any> PropertyProvider<T>.nullable(): PropertyProvider<T?> =
        property(Type.Nullable(type), isExtension)

    protected fun <T> PropertyProvider<T>.optional(): PropertyProvider<Option<T>> =
        property(Type.Optional(type), isExtension)

    protected fun <T> list(prop: PropertyProvider<T>): PropertyProvider<List<T>> =
        property(Type.ArrayType(prop.type), prop.isExtension)

    protected fun <V> map(valueProp: PropertyProvider<V>): PropertyProvider<Map<String, V>> {
        return property(Type.MapType(valueProp.type), valueProp.isExtension)
    }

    protected fun <T : Any> obj(structure: TypeStructure<T>, isExtension: Boolean = false): PropertyProvider<T> {
        return property(Type.ObjectType(structure), isExtension)
    }

    protected inline fun <reified T : Enum<T>> enum(isExtension: Boolean = false): PropertyProvider<T> =
        property(Type.EnumType(), isExtension)

    protected fun <T> property(type: Type<T>, isExtension: Boolean = false): PropertyProvider<T> =
        PropertyProvider(type, isExtension, isRecord)

    protected fun JsonValue.withClassName(className: String): JsonValue = also {
        if (it.getField("className") == null) {
            it["className"] = jsonString(className)
        }
    }

    protected fun compactIdToFieldNamesAndJson(compactId: String): Pair<Set<String>, JsonValue> {
        fun getFieldToValue(component: String): Pair<String, JsonValue> =
            component.substringBefore(':') to
                (component.substringAfter(':').takeIf { it != component }?.let(::jsonString) ?: jsonNull())

        if (!compactId.startsWith('{')) {
            val fieldToValue = getFieldToValue(compactId)
            return setOf(fieldToValue.first) to jsonObject(fieldToValue)
        }

        val resultMap = mutableMapOf<String, JsonValue>()
        var prevIndex = 0
        var depth = 0
        (compactId.removePrefix("{").removeSuffix("}") + ',').forEachIndexed { i, c ->
            when (c) {
                ',' -> {
                    if (depth == 0) {
                        resultMap += getFieldToValue(compactId.substring(prevIndex, i))
                        prevIndex = i + 1
                    }
                }

                '{' -> depth++
                '}' -> depth--
            }
        }
        return resultMap.keys to jsonObject(resultMap)
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
    ) {
        public fun toProperty(name: String): Property<T> {
            val inclusionStrategy = when {
                isExtension -> InclusionStrategy.ONLY_EXPLICIT
                isOuterRecord && name == "id" -> InclusionStrategy.ONLY_EXPLICIT_OR_PARENT_EXPLICIT
                else -> InclusionStrategy.PARENT
            }
            return Property(name, type, inclusionStrategy)
        }
    }
}
