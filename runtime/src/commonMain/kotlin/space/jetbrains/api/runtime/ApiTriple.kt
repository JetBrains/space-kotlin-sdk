package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.Partial.Special
import space.jetbrains.api.runtime.PropertyValue.Value
import space.jetbrains.api.runtime.Type.Nullable
import space.jetbrains.api.runtime.Type.ObjectType
import kotlin.js.*
import kotlin.jvm.*

class ApiTriple<out A, out B, out C>(first: PropertyValue<A>, second: PropertyValue<B>, third: PropertyValue<C>) {
    val first by first
    val second by second
    val third by third

    constructor(first: A, second: B, third: C) : this(
        Value(first),
        Value(second),
        Value(third)
    )

    fun toTriple(): Triple<A, B, C> = Triple(first, second, third)
}

fun <A, B, C> Triple<A, B, C>.toApiTriple(): ApiTriple<A, B, C> = ApiTriple(first, second, third)

class ApiTripleStructure<A, B, C>(typeA: Type<A>, typeB: Type<B>, typeC: Type<C>) : TypeStructure<ApiTriple<A, B, C>>() {
    val first by property(typeA)
    val second by property(typeB)
    val third by property(typeC)

    override fun deserialize(context: DeserializationContext<in ApiTriple<A, B, C>>): ApiTriple<A, B, C> = ApiTriple(
        first.deserialize(context),
        second.deserialize(context),
        third.deserialize(context)
    )

    override fun serialize(value: ApiTriple<A, B, C>): JsonValue = jsonObject(listOfNotNull(
        first.serialize(value.first),
        second.serialize(value.second),
        third.serialize(value.third)
    ))

    override val defaultPartialFull: Partial<in ApiTriple<A, B, C>>.() -> Unit = {
        addImplicitPartial(first)
        addImplicitPartial(second)
        addImplicitPartial(third)
    }
}


@JvmName("partial-ApiTriple-first")
fun <A> Partial<ApiTriple<A, *, *>>.first(): Unit = add((structure as ApiTripleStructure<A, *, *>).first)

@JvmName("partial-ApiTriple-first")
fun <A : Any> Partial<ApiTriple<A, *, *>>.first(buildPartial: Partial<A>.() -> Unit) {
    val property = (structure as ApiTripleStructure).first
    add(property, (property.type as ObjectType).structure, buildPartial)
}

@JvmName("partial-ApiTriple-first-nullable")
@JsName("partial_ApiTriple_first_nullable")
fun <A : Any> Partial<ApiTriple<A?, *, *>>.first(buildPartial: Partial<A>.() -> Unit) {
    val property = (structure as ApiTripleStructure).first
    add(property, ((property.type as Nullable).type as ObjectType).structure, buildPartial)
}

@JvmName("partial-ApiTriple-first-custom")
fun <T : Any> Partial<ApiTriple<*, *, *>>.firstCustom(
    structure: TypeStructure<T>,
    special: Special? = null,
    buildPartial: Partial<T>.() -> Unit
) {
    val property = (this.structure as ApiTripleStructure).first
    add(property, structure, buildPartial, special)
}


@JvmName("partial-ApiTriple-second")
fun <B> Partial<ApiTriple<*, B, *>>.second(): Unit = add((structure as ApiTripleStructure).second)

@JvmName("partial-ApiTriple-second")
fun <B : Any> Partial<ApiTriple<*, B, *>>.second(buildPartial: Partial<B>.() -> Unit) {
    val property = (structure as ApiTripleStructure).second
    add(property, (property.type as ObjectType).structure, buildPartial)
}

@JvmName("partial-ApiTriple-second-nullable")
@JsName("partial_ApiTriple_second_nullable")
fun <B : Any> Partial<ApiTriple<*, B?, *>>.second(buildPartial: Partial<B>.() -> Unit) {
    val property = (structure as ApiTripleStructure).second
    add(property, ((property.type as Nullable).type as ObjectType).structure, buildPartial)
}

@JvmName("partial-ApiTriple-second-custom")
fun <T : Any> Partial<ApiTriple<*, *, *>>.secondCustom(
    structure: TypeStructure<T>,
    special: Special? = null,
    buildPartial: Partial<T>.() -> Unit
) {
    val property = (this.structure as ApiTripleStructure).second
    add(property, structure, buildPartial, special)
}


@JvmName("partial-ApiTriple-third")
fun <C> Partial<ApiTriple<*, *, C>>.third(): Unit = add((structure as ApiTripleStructure).third)

@JvmName("partial-ApiTriple-third")
fun <C : Any> Partial<ApiTriple<*, *, C>>.third(buildPartial: Partial<C>.() -> Unit) {
    val property = (structure as ApiTripleStructure).third
    add(property, (property.type as ObjectType).structure, buildPartial)
}

@JvmName("partial-ApiTriple-third-nullable")
@JsName("partial_ApiTriple_third_nullable")
fun <C : Any> Partial<ApiTriple<*, *, C?>>.third(buildPartial: Partial<C>.() -> Unit) {
    val property = (structure as ApiTripleStructure).third
    add(property, ((property.type as Nullable).type as ObjectType).structure, buildPartial)
}

@JvmName("partial-ApiTriple-third-custom")
fun <T : Any> Partial<ApiTriple<*, *, *>>.thirdCustom(
    structure: TypeStructure<T>,
    special: Special? = null,
    buildPartial: Partial<T>.() -> Unit
) {
    val property = (this.structure as ApiTripleStructure).third
    add(property, structure, buildPartial, special)
}
