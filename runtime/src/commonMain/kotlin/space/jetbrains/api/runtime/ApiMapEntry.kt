package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PropertyValue.Value

internal class ApiMapEntry<out K, out V>(key: PropertyValue<K>, value: PropertyValue<V>) {
    val key by key
    val value by value

    constructor(key: K, value: V) : this(Value(key), Value(value))
}

internal class ApiMapEntryStructure<K, V>(keyType: Type<K>, valueType: Type<V>) : TypeStructure<ApiMapEntry<K, V>>() {
    val key by property(keyType)
    val value by property(valueType)

    override fun deserialize(context: DeserializationContext): Nothing
        = throw UnsupportedOperationException("No partials for map entries")

    override fun serialize(value: ApiMapEntry<K, V>): JsonValue = jsonObject(listOfNotNull(
        key.serialize(value.key),
        this.value.serialize(value.value)
    ))
}
