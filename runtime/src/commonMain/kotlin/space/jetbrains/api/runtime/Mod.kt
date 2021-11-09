package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PropertyValue.Value

public class Mod<out T : Any>(old: PropertyValue<T?>, new: PropertyValue<T?>) {
    public val old: T? by old
    public val new: T? by new

    public constructor(old: T?, new: T?) : this(Value(old), Value(new))
}

public class ModStructure<T : Any>(type: Type<T>) : TypeStructure<Mod<T>>(isRecord = false) {
    private val old: Property<T?> by property(type).nullable()
    private val new: Property<T?> by property(type).nullable()

    override fun deserialize(context: DeserializationContext): Mod<T> = Mod(
        old.deserialize(context),
        new.deserialize(context)
    )

    override fun serialize(value: Mod<T>): JsonValue = jsonObject(listOfNotNull(
        old.serialize(value.old),
        new.serialize(value.new)
    ))
}

public interface ModPartial<out T : Partial> : Partial {
    public fun old()
    public fun old(buildPartial: T.() -> Unit)
    public fun new()
    public fun new(buildPartial: T.() -> Unit)
}

public class ModPartialImpl<out T : Partial>(
    private val t: (PartialBuilder.Explicit) -> T,
    builder: PartialBuilder.Explicit
) : PartialImpl(builder), ModPartial<T> {
    override fun old(): Unit = builder.add("old")

    override fun old(buildPartial: T.() -> Unit) {
        builder.add("old", {
            t(it).buildPartial()
        })
    }

    override fun new(): Unit = builder.add("new")

    override fun new(buildPartial: T.() -> Unit) {
        builder.add("new", {
            t(it).buildPartial()
        })
    }
}
