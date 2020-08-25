package space.jetbrains.api.runtime

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import space.jetbrains.api.runtime.Type.NumberType.IntType
import space.jetbrains.api.runtime.Type.NumberType.LongType
import space.jetbrains.api.runtime.Type.PrimitiveType.StringType

public sealed class Type<T> {
    public abstract fun deserialize(context: DeserializationContext): T
    public abstract fun serialize(value: T): JsonValue?

    public sealed class NumberType<T : Number> : Type<T>() {
        protected abstract fun fromNumber(number: Number): T

        override fun deserialize(context: DeserializationContext): T {
            return fromNumber(context.requireJson().asNumber(context.link))
        }

        override fun serialize(value: T): JsonValue = jsonNumber(value)

        public object ByteType : NumberType<Byte>() {
            override fun fromNumber(number: Number): Byte = number.toByte()
        }

        public object ShortType : NumberType<Short>() {
            override fun fromNumber(number: Number): Short = number.toShort()
        }

        public object IntType : NumberType<Int>() {
            override fun fromNumber(number: Number): Int = number.toInt()
        }

        public object LongType : NumberType<Long>() {
            override fun fromNumber(number: Number): Long = number.toLong()
        }

        public object FloatType : NumberType<Float>() {
            override fun fromNumber(number: Number): Float = number.toFloat()
        }

        public object DoubleType : NumberType<Double>() {
            override fun fromNumber(number: Number): Double = number.toDouble()
        }
    }

    public sealed class PrimitiveType<T : Any> : Type<T>() {
        public object BooleanType : PrimitiveType<Boolean>() {
            override fun deserialize(context: DeserializationContext): Boolean = context.requireJson().asBoolean(context.link)
            override fun serialize(value: Boolean): JsonValue = jsonBoolean(value)
        }

        public object StringType : PrimitiveType<String>() {
            override fun deserialize(context: DeserializationContext): String = context.requireJson().asString(context.link)
            override fun serialize(value: String): JsonValue = jsonString(value)
        }

        public object DateType : PrimitiveType<LocalDate>() {
            override fun deserialize(context: DeserializationContext): LocalDate {
                return LocalDate.parse(StringType.deserialize(context.child("iso")))
            }

            override fun serialize(value: LocalDate): JsonValue {
                return jsonObject("iso" to jsonString(value.toString()))
            }
        }

        public object DateTimeType : PrimitiveType<Instant>() {
            override fun deserialize(context: DeserializationContext): Instant {
                return Instant.fromEpochMilliseconds(LongType.deserialize(context.child("timestamp")))
            }

            override fun serialize(value: Instant): JsonValue {
                return jsonObject("timestamp" to jsonNumber(value.toEpochMilliseconds()))
            }
        }
    }

    public class Nullable<T : Any>(public val type: Type<T>) : Type<T?>() {
        override fun deserialize(context: DeserializationContext): T? {
            return if (context.json != null && !context.json.isNull()) {
                type.deserialize(context)
            }
            else null
        }

        override fun serialize(value: T?): JsonValue {
            return value?.let { type.serialize(it) } ?: jsonNull()
        }
    }

    public class Optional<T>(public val type: Type<T>) : Type<Option<T>>() {
        override fun deserialize(context: DeserializationContext): Option<T> {
            return if (context.json != null) {
                Option.Value(type.deserialize(context))
            }
            else Option.None
        }

        override fun serialize(value: Option<T>): JsonValue? {
            return (value as? Option.Value)?.let { type.serialize(it.value) }
        }
    }

    public class ArrayType<T>(public val elementType: Type<T>) : Type<List<T>>() {
        override fun deserialize(context: DeserializationContext): List<T> = context.elements().map {
            elementType.deserialize(it)
        }

        override fun serialize(value: List<T>): JsonValue {
            return jsonArray(*Array(value.size) { elementType.serialize(value[it])!! })
        }
    }

    public class MapType<V>(public val valueType: Type<V>) : Type<Map<String, V>>() {
        override fun deserialize(context: DeserializationContext): Map<String, V> {
            return context.requireJson().getFields(context.link).associate { (key, json) ->
                key to valueType.deserialize(context.child("[\"$key\"]", json))
            }
        }

        override fun serialize(value: Map<String, V>): JsonValue {
            return jsonObject(value.map {
                it.key to (valueType.serialize(it.value) ?: error("Map values cannot be optional"))
            })
        }
    }

    public class BatchType<T>(public val elementType: Type<T>) : Type<Batch<T>>() {
        private val arrayType = ArrayType(elementType)

        override fun deserialize(context: DeserializationContext): Batch<T> {
            return Batch(
                StringType.deserialize(context.child("next")),
                Nullable(IntType).deserialize(context.child("totalCount")),
                arrayType.deserialize(context.child("data"))
            )
        }

        override fun serialize(value: Batch<T>): JsonValue {
            return jsonObject(
                "next" to jsonString(value.next),
                "totalCount" to (value.totalCount?.let { jsonNumber(it) } ?: jsonNull()),
                "data" to arrayType.serialize(value.data)
            )
        }
    }

    public class ObjectType<T : Any>(public val structure: TypeStructure<T>) : Type<T>() {
        override fun deserialize(context: DeserializationContext): T {
            context.requireJson()
            @Suppress("UNCHECKED_CAST")
            return structure.deserialize(context)
        }

        override fun serialize(value: T): JsonValue = structure.serialize(value)
    }

    public class EnumType<T : Enum<T>> @PublishedApi internal constructor(private val values: List<T>) : Type<T>() {
        override fun deserialize(context: DeserializationContext): T {
            val name = StringType.deserialize(context)
            return values.first { it.name == name }
        }

        override fun serialize(value: T): JsonValue = jsonString(value.name)

        public companion object {
            public inline operator fun <reified T : Enum<T>> invoke(): EnumType<T> = EnumType(enumValues<T>().asList())
        }
    }

    private fun partialStructure(): TypeStructure<*>? = when (this) {
        is NumberType, is PrimitiveType, is EnumType -> null
        is Nullable<*> -> type.partialStructure()
        is Optional<*> -> type.partialStructure()
        is ArrayType<*> -> elementType.partialStructure()
        is MapType<*> -> valueType.partialStructure()
        is BatchType<*> -> elementType.partialStructure()
        is ObjectType -> structure
    }
}
