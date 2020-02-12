package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.Partial.Special
import space.jetbrains.api.runtime.PropertyValue.Value
import space.jetbrains.api.runtime.Type.Nullable
import space.jetbrains.api.runtime.Type.ObjectType
import kotlin.jvm.*

class Mod<out T : Any>(old: PropertyValue<T?>, new: PropertyValue<T?>) {
    val old by old
    val new by new

    constructor(old: T?, new: T?) : this(Value(old), Value(new))
}

class ModStructure<T : Any>(type: Type<T>) : TypeStructure<Mod<T>>() {
    val old by property(type).nullable()
    val new by property(type).nullable()

    override fun deserialize(context: DeserializationContext<in Mod<T>>): Mod<T> = Mod(
        old.deserialize(context),
        new.deserialize(context)
    )

    override fun serialize(value: Mod<T>): JsonValue = jsonObject(listOfNotNull(
        old.serialize(value.old),
        new.serialize(value.new)
    ))

    override val defaultPartialFull: Partial<in Mod<T>>.() -> Unit = {
        addImplicitPartial(old)
        addImplicitPartial(new)
    }
}


@JvmName("partial-Mod-old")
fun <T : Any> Partial<Mod<T>>.old() = add((structure as ModStructure).old)

@JvmName("partial-Mod-old")
fun <T : Any> Partial<Mod<T>>.old(buildPartial: Partial<T>.() -> Unit) {
    val property = (structure as ModStructure).old
    add(property, ((property.type as Nullable).type as ObjectType).structure, buildPartial)
}

@JvmName("partial-Mod-old-custom")
fun <T : Any> Partial<Mod<*>>.oldCustom(
    structure: TypeStructure<T>,
    special: Special? = null,
    buildPartial: Partial<T>.() -> Unit
) {
    val property = (this.structure as ModStructure).old
    add(property, structure, buildPartial, special)
}


@JvmName("partial-Mod-new")
fun <T : Any> Partial<Mod<T>>.new() = add((structure as ModStructure).new)

@JvmName("partial-Mod-new")
fun <T : Any> Partial<Mod<T>>.new(buildPartial: Partial<T>.() -> Unit) {
    val property = (structure as ModStructure).new
    add(property, ((property.type as Nullable).type as ObjectType).structure, buildPartial)
}

@JvmName("partial-Mod-new-custom")
fun <T : Any> Partial<Mod<*>>.newCustom(
    structure: TypeStructure<T>,
    special: Special? = null,
    buildPartial: Partial<T>.() -> Unit
) {
    val property = (this.structure as ModStructure).new
    add(property, structure, buildPartial, special)
}
