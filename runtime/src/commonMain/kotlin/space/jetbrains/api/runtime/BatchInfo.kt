package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PropertyValue.Value

class BatchInfo(
    offset: PropertyValue<String?>,
    batchSize: PropertyValue<Int>
) {
    val offset by offset
    val batchSize by batchSize

    constructor(offset: String?, batchSize: Int) : this(Value(offset), Value(batchSize))
}

object BatchInfoStructure : TypeStructure<BatchInfo>() {
    val offset by string().nullable()
    val batchSize by int()

    override fun deserialize(context: DeserializationContext): BatchInfo = BatchInfo(
        offset = offset.deserialize(context),
        batchSize = batchSize.deserialize(context)
    )

    override fun serialize(value: BatchInfo): JsonValue = jsonObject(listOfNotNull(
        offset.serialize(value.offset),
        batchSize.serialize(value.batchSize)
    ))
}
