package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PropertyValue.Value

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

    override fun deserialize(context: DeserializationContext): ApiTriple<A, B, C> = ApiTriple(
        first.deserialize(context),
        second.deserialize(context),
        third.deserialize(context)
    )

    override fun serialize(value: ApiTriple<A, B, C>): JsonValue = jsonObject(listOfNotNull(
        first.serialize(value.first),
        second.serialize(value.second),
        third.serialize(value.third)
    ))
}


interface ApiTriplePartial<out A : Partial, out B : Partial, out C : Partial> : Partial {
    fun first()
    fun first(buildPartial: A.() -> Unit)
    fun second()
    fun second(buildPartial: B.() -> Unit)
    fun third()
    fun third(buildPartial: C.() -> Unit)
}

class ApiTriplePartialImpl<out A : Partial, out B : Partial, out C : Partial>(
    private val a: (PartialBuilder) -> A,
    private val b: (PartialBuilder) -> B,
    private val c: (PartialBuilder) -> C,
    builder: PartialBuilder
) : PartialImpl(builder), ApiTriplePartial<A, B, C> {
    override fun first(): Unit = builder.add("first")

    override fun first(buildPartial: A.() -> Unit) {
        builder.add("first", {
            a(it).buildPartial()
        })
    }

    override fun second(): Unit = builder.add("second")

    override fun second(buildPartial: B.() -> Unit) {
        builder.add("second", {
            b(it).buildPartial()
        })
    }

    override fun third(): Unit = builder.add("third")

    override fun third(buildPartial: C.() -> Unit) {
        builder.add("first", {
            c(it).buildPartial()
        })
    }
}
