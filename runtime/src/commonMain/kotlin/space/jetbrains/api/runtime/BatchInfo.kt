@file:Suppress("PrivatePropertyName")

package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PropertyValue.Value

public class BatchInfo(
    offset: PropertyValue<String?>,
    batchSize: PropertyValue<Int>
) {
    private val __offset: PropertyValue<String?> = offset
    public val offset: String? get() = __offset.getValue("offset")
    private val __batchSize: PropertyValue<Int> = batchSize
    public val batchSize: Int get() = __batchSize.getValue("batchSize")

    public constructor(offset: String?, batchSize: Int) : this(Value(offset), Value(batchSize))
}

public object BatchInfoStructure : TypeStructure<BatchInfo>(isRecord = false) {
    private val offset: Property<String?> = string().nullable().toProperty("offset")
    private val batchSize: Property<Int> = int().toProperty("batchSize")

    override fun deserialize(context: DeserializationContext): BatchInfo = BatchInfo(
        offset = offset.deserialize(context),
        batchSize = batchSize.deserialize(context)
    )

    override fun serialize(value: BatchInfo): JsonValue = jsonObject(listOfNotNull(
        offset.serialize(value.offset),
        batchSize.serialize(value.batchSize)
    ))
}
