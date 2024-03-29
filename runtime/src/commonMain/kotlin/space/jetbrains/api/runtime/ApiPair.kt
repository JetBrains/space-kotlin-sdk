@file:Suppress("PrivatePropertyName")

package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PropertyValue.Value

public class ApiPair<out A, out B>(first: PropertyValue<A>, second: PropertyValue<B>) {
    private val __first: PropertyValue<A> = first
    public val first: A get() = __first.getValue("first")
    private val __second: PropertyValue<B> = second
    public val second: B get() = __second.getValue("second")

    public constructor(first: A, second: B) : this(Value(first), Value(second))

    public fun toPair(): Pair<A, B> = first to second
}

public fun <A, B> Pair<A, B>.toApiPair(): ApiPair<A, B> = ApiPair(first, second)

public class ApiPairStructure<A, B>(typeA: Type<A>, typeB: Type<B>) : TypeStructure<ApiPair<A, B>>(
    isRecord = false
) {
    private val first: Property<A> = property(typeA).toProperty("first")
    private val second: Property<B> = property(typeB).toProperty("second")

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

public interface ApiPairPartial<out A : Partial, out B : Partial> : Partial {
    public fun first()
    public fun first(buildPartial: A.() -> Unit)
    public fun second()
    public fun second(buildPartial: B.() -> Unit)
}

public class ApiPairPartialImpl<out A : Partial, out B : Partial>(
    private val a: (PartialBuilder.Explicit) -> A,
    private val b: (PartialBuilder.Explicit) -> B,
    builder: PartialBuilder.Explicit
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
