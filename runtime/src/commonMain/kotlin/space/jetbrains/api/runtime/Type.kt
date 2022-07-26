package space.jetbrains.api.runtime

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import mu.KotlinLogging
import space.jetbrains.api.runtime.Type.NumberType.IntType
import space.jetbrains.api.runtime.Type.NumberType.LongType
import space.jetbrains.api.runtime.Type.PrimitiveType.BooleanType
import space.jetbrains.api.runtime.Type.PrimitiveType.StringType
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

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
            override fun deserialize(context: DeserializationContext): Boolean =
                context.requireJson().asBoolean(context.link)

            override fun serialize(value: Boolean): JsonValue = jsonBoolean(value)
        }

        public object StringType : PrimitiveType<String>() {
            override fun deserialize(context: DeserializationContext): String =
                context.requireJson().asString(context.link)

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

        @OptIn(ExperimentalTime::class)
        public object DurationType : PrimitiveType<Duration>() {
            override fun deserialize(context: DeserializationContext): Duration {
                return Duration.parse(context.requireJson().asString(context.link))
            }

            override fun serialize(value: Duration): JsonValue {
                return jsonString(value.toIsoString())
            }
        }
    }

    public class Nullable<T : Any>(public val type: Type<T>) : Type<T?>() {
        override fun deserialize(context: DeserializationContext): T? {
            return if (context.json != null && !context.json.isNull()) {
                tryDeserialize(context.link) {
                    type.deserialize(context)
                }.valueOrNull
            } else null
        }

        override fun serialize(value: T?): JsonValue {
            return value?.let { type.serialize(it) } ?: jsonNull()
        }
    }

    public class Optional<T>(public val type: Type<T>) : Type<Option<T>>() {
        override fun deserialize(context: DeserializationContext): Option<T> {
            return if (context.json != null) {
                Option.Value(type.deserialize(context))
            } else Option.None
        }

        override fun serialize(value: Option<T>): JsonValue? {
            return (value as? Option.Value)?.let { type.serialize(it.value) }
        }
    }

    public class ArrayType<T>(public val elementType: Type<T>) : Type<List<T>>() {
        override fun deserialize(context: DeserializationContext): List<T> = mutableListOf<T>().apply {
            context.elements().forEach {
                tryDeserialize(context.link) {
                    elementType.deserialize(it)
                }.ifValue { add(it) }
            }
        }

        override fun serialize(value: List<T>): JsonValue {
            return jsonArray(*Array(value.size) { elementType.serialize(value[it])!! })
        }
    }

    public class MapType<V>(public val valueType: Type<V>) : Type<Map<String, V>>() {
        override fun deserialize(context: DeserializationContext): Map<String, V> {
            return mutableMapOf<String, V>().apply {
                context.requireJson().getFields(context.link).forEach { (key, json) ->
                    val elemContext = context.child("[\"$key\"]", json, partial = context.partial)
                    tryDeserialize(elemContext.link) {
                        valueType.deserialize(elemContext)
                    }.ifValue { put(key, it) }
                }
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
                StringType.deserialize(context.child("next", partial = context.partial)),
                Nullable(IntType).deserialize(context.child("totalCount", partial = context.partial)),
                arrayType.deserialize(context.child("data", partial = context.partial))
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

    public class SyncBatchType<T>(public val elementType: Type<T>) : Type<SyncBatch<T>>() {
        private val arrayType = ArrayType(elementType)

        override fun deserialize(context: DeserializationContext): SyncBatch<T> {
            return SyncBatch(
                etag = StringType.deserialize(context.child("etag", partial = context.partial)),
                data = arrayType.deserialize(context.child("data", partial = context.partial)),
                hasMore = BooleanType.deserialize(context.child("hasMore", partial = context.partial))
            )
        }

        override fun serialize(value: SyncBatch<T>): JsonValue {
            return jsonObject(
                "etag" to jsonString(value.etag),
                "data" to arrayType.serialize(value.data),
                "hasMore" to jsonBoolean(value.hasMore)
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
        is SyncBatchType<*> -> elementType.partialStructure()
        is ObjectType -> structure
    }

    private companion object {
        val log = KotlinLogging.logger {}

        inline fun <T> tryDeserialize(link: ReferenceChainLink, deserialize: () -> T): Option<T> {
            return try {
                Option.Value(deserialize())
            } catch (e: DeserializationException) {
                val msg = "Deserialization failed. Setting " + link.referenceChain() + " to null\n" +
                        "Error: " + e.message
                when (e) {
                    is DeserializationException.Major -> log.error { msg }
                    is DeserializationException.Minor -> {
                        if (e.link === link || e.link.parent === link && e.link.name.startsWith("[")) {
                            log.warn { msg }
                        } else {
                            log.error { msg }
                        }
                    }
                }
                Option.None
            }
        }
    }
}
