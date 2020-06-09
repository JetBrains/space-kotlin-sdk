package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PropertyValue.Value

class Mod<out T : Any>(old: PropertyValue<T?>, new: PropertyValue<T?>) {
    val old by old
    val new by new

    constructor(old: T?, new: T?) : this(Value(old), Value(new))
}

class ModStructure<T : Any>(type: Type<T>) : TypeStructure<Mod<T>>() {
    val old by property(type).nullable()
    val new by property(type).nullable()

    override fun deserialize(context: DeserializationContext): Mod<T> = Mod(
        old.deserialize(context),
        new.deserialize(context)
    )

    override fun serialize(value: Mod<T>): JsonValue = jsonObject(listOfNotNull(
        old.serialize(value.old),
        new.serialize(value.new)
    ))
}

interface ModPartial<out T : Partial> : Partial {
    fun old()
    fun old(buildPartial: T.() -> Unit)
    fun new()
    fun new(buildPartial: T.() -> Unit)
}

class ModPartialImpl<out T : Partial>(
    private val t: (PartialBuilder) -> T,
    builder: PartialBuilder
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
