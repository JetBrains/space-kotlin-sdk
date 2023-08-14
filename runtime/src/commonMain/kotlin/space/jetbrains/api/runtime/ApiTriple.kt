@file:Suppress("PrivatePropertyName")

package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PropertyValue.Value

public class ApiTriple<out A, out B, out C>(first: PropertyValue<A>, second: PropertyValue<B>, third: PropertyValue<C>) {
    private val __first: PropertyValue<A> = first
    public val first: A get() = __first.getValue("first")
    private val __second: PropertyValue<B> = second
    public val second: B get() = __second.getValue("second")
    private val __third: PropertyValue<C> = third
    public val third: C get() = __third.getValue("third")

    public constructor(first: A, second: B, third: C) : this(
        Value(first),
        Value(second),
        Value(third)
    )

    public fun toTriple(): Triple<A, B, C> = Triple(first, second, third)
}

public fun <A, B, C> Triple<A, B, C>.toApiTriple(): ApiTriple<A, B, C> = ApiTriple(first, second, third)

public class ApiTripleStructure<A, B, C>(typeA: Type<A>, typeB: Type<B>, typeC: Type<C>) :
    TypeStructure<ApiTriple<A, B, C>>(isRecord = false) {
    private val first: Property<A> = property(typeA).toProperty("first")
    private val second: Property<B> = property(typeB).toProperty("second")
    private val third: Property<C> = property(typeC).toProperty("third")

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


public interface ApiTriplePartial<out A : Partial, out B : Partial, out C : Partial> : Partial {
    public fun first()
    public fun first(buildPartial: A.() -> Unit)
    public fun second()
    public fun second(buildPartial: B.() -> Unit)
    public fun third()
    public fun third(buildPartial: C.() -> Unit)
}

public class ApiTriplePartialImpl<out A : Partial, out B : Partial, out C : Partial>(
    private val a: (PartialBuilder.Explicit) -> A,
    private val b: (PartialBuilder.Explicit) -> B,
    private val c: (PartialBuilder.Explicit) -> C,
    builder: PartialBuilder.Explicit
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
