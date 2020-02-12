package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.Partial.Special
import space.jetbrains.api.runtime.PropertyValue.Value
import space.jetbrains.api.runtime.Type.Nullable
import space.jetbrains.api.runtime.Type.ObjectType
import kotlin.js.*
import kotlin.jvm.*

class ApiPair<out A, out B>(first: PropertyValue<A>, second: PropertyValue<B>) {
    val first by first
    val second by second

    constructor(first: A, second: B) : this(Value(first), Value(second))

    fun toPair(): Pair<A, B> = first to second
}

fun <A, B> Pair<A, B>.toApiPair(): ApiPair<A, B> = ApiPair(first, second)

class ApiPairStructure<A, B>(typeA: Type<A>, typeB: Type<B>) : TypeStructure<ApiPair<A, B>>() {
    val first by property(typeA)
    val second by property(typeB)

    override fun deserialize(context: DeserializationContext<in ApiPair<A, B>>): ApiPair<A, B> = ApiPair(
        first.deserialize(context),
        second.deserialize(context)
    )

    override fun serialize(value: ApiPair<A, B>): JsonValue = jsonObject(listOfNotNull(
        first.serialize(value.first),
        second.serialize(value.second)
    ))

    override val defaultPartialFull: Partial<in ApiPair<A, B>>.() -> Unit = {
        addImplicitPartial(first)
        addImplicitPartial(second)
    }
}

@JvmName("partial-ApiPair-first")
fun <A> Partial<ApiPair<A, *>>.first() = add((structure as ApiPairStructure).first)

@JvmName("partial-ApiPair-first")
fun <A : Any> Partial<ApiPair<A, *>>.first(buildPartial: Partial<A>.() -> Unit) {
    val property = (structure as ApiPairStructure).first
    add(property, (property.type as ObjectType).structure, buildPartial)
}

@JvmName("partial-ApiPair-first-nullable")
@JsName("partial_ApiPair_first_nullable")
fun <A : Any> Partial<ApiPair<A?, *>>.first(buildPartial: Partial<A>.() -> Unit) {
    val property = (structure as ApiPairStructure).first
    add(property, ((property.type as Nullable).type as ObjectType).structure, buildPartial)
}

@JvmName("partial-ApiPair-first-custom")
fun <T : Any> Partial<ApiPair<*, *>>.firstCustom(
    structure: TypeStructure<T>,
    special: Special? = null,
    buildPartial: Partial<T>.() -> Unit
) {
    val property = (this.structure as ApiPairStructure).first
    add(property, structure, buildPartial, special)
}


@JvmName("partial-ApiPair-second")
fun <B> Partial<ApiPair<*, B>>.second() = add((structure as ApiPairStructure<*, B>).second)

@JvmName("partial-ApiPair-second")
fun <B : Any> Partial<ApiPair<*, B>>.second(buildPartial: Partial<B>.() -> Unit) {
    val property = (structure as ApiPairStructure).second
    add(property, (property.type as ObjectType).structure, buildPartial)
}

@JvmName("partial-ApiPair-second-nullable")
@JsName("partial_ApiPair_second_nullable")
fun <B : Any> Partial<ApiPair<*, B?>>.second(buildPartial: Partial<B>.() -> Unit) {
    val property = (structure as ApiPairStructure).second
    add(property, ((property.type as Nullable).type as ObjectType).structure, buildPartial)
}

@JvmName("partial-ApiPair-second-custom")
fun <T : Any> Partial<ApiPair<*, *>>.secondCustom(
    structure: TypeStructure<T>,
    special: Special? = null,
    buildPartial: Partial<T>.() -> Unit
) {
    val property = (this.structure as ApiPairStructure).second
    add(property, structure, buildPartial, special)
}
