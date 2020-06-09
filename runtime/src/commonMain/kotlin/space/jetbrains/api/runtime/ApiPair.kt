package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PropertyValue.Value

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

    override fun deserialize(context: DeserializationContext): ApiPair<A, B> = ApiPair(
        first.deserialize(context),
        second.deserialize(context)
    )

    override fun serialize(value: ApiPair<A, B>): JsonValue = jsonObject(
        listOfNotNull(
            first.serialize(value.first),
            second.serialize(value.second)
        )
    )
}

interface ApiPairPartial<out A : Partial, out B : Partial> : Partial {
    fun first()
    fun first(buildPartial: A.() -> Unit)
    fun second()
    fun second(buildPartial: B.() -> Unit)
}

class ApiPairPartialImpl<out A : Partial, out B : Partial>(
    private val a: (PartialBuilder) -> A,
    private val b: (PartialBuilder) -> B,
    builder: PartialBuilder
) : PartialImpl(builder), ApiPairPartial<A, B> {
    override fun first(): Unit = builder.add("first")

    override fun first(buildPartial: A.() -> Unit) {
        builder.add("first", {
            a(it).buildPartial()
        })
    }

    override fun second(): Unit = builder.add("second")

    override fun second(buildPartial: B.() -> Unit) {
        builder.add("first", {
            b(it).buildPartial()
        })
    }
}
