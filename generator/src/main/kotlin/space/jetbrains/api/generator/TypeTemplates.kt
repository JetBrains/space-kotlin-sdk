package space.jetbrains.api.generator

import space.jetbrains.api.generator.HA_Type.Object.Kind.*
import com.squareup.kotlinpoet.*

fun CodeBlock.Builder.appendStructure(type: HA_Type, model: HttpApiEntitiesById): CodeBlock.Builder {
    return when (type) {
        is HA_Type.Primitive -> error("Primitives have no structure")
        is HA_Type.Array -> error("Arrays have no structure")
        is HA_Type.Map -> error("Maps have no structure")
        is HA_Type.Object -> when (type.kind) {
            PAIR -> {
                add("%T(", apiPairStructureType)
                appendFieldType(type.firstField(), model)
                add(", ")
                appendFieldType(type.secondField(), model)
                add(")")
            }
            TRIPLE -> {
                add("%T(", apiTripleStructureType)
                appendFieldType(type.firstField(), model)
                add(", ")
                appendFieldType(type.secondField(), model)
                add(", ")
                appendFieldType(type.thirdField(), model)
                add(")")
            }
            BATCH, SYNC_BATCH -> error("Batches have no structure")
            MOD -> {
                add("%T(", modStructureType)
                appendType(type.modSubjectType(), model, false)
                add(")")
            }
            REQUEST_BODY -> error("Request bodies cannot appear in parameters")
        }
        is HA_Type.Dto -> {
            add("%T", model.resolveDto(type).getClassName().getStructureClassName())
        }
        is HA_Type.Ref -> {
            add("%T", model.resolveDto(type).getClassName().getStructureClassName())
        }
        is HA_Type.Enum -> error("Enums have no structure")
        is HA_Type.UrlParam -> {
            add("%T", model.resolveUrlParam(type).getClassName().getStructureClassName())
        }
    }
}

fun ClassName.importNested() = ClassName(
    packageName = packageName + "." + simpleNames.dropLast(1).joinToString("."),
    simpleNames = listOf(simpleName)
)

fun CodeBlock.Builder.appendFieldType(field: HA_Field, model: HttpApiEntitiesById) =
    appendType(field.type, model, field.requiresOption)

fun CodeBlock.Builder.appendType(
    type: HA_Type,
    model: HttpApiEntitiesById,
    option: Boolean
): CodeBlock.Builder {
    if (option) add("%T(", optionalType.importNested())
    if (type.nullable) add("%T(", nullableType.importNested())
    when (val notNullType = type.copy(nullable = false)) {
        is HA_Type.Primitive -> {
            add(
                "%T", when (notNullType.primitive) {
                    HA_Primitive.Byte -> byteTypeType.importNested()
                    HA_Primitive.Short -> shortTypeType.importNested()
                    HA_Primitive.Int -> intTypeType.importNested()
                    HA_Primitive.Long -> longTypeType.importNested()
                    HA_Primitive.Float -> floatTypeType.importNested()
                    HA_Primitive.Double -> doubleTypeType.importNested()
                    HA_Primitive.Boolean -> booleanTypeType.importNested()
                    HA_Primitive.String -> stringTypeType.importNested()
                    HA_Primitive.Date -> dateTypeType.importNested()
                    HA_Primitive.DateTime -> dateTimeTypeType.importNested()
                }
            )
        }
        is HA_Type.Array -> {
            add("%T(", arrayTypeType.importNested())
            appendType(notNullType.elementType, model, false)
            add(")")
        }
        is HA_Type.Map -> {
            add("%T(", mapTypeType.importNested())
            appendType(notNullType.valueType, model, false)
            add(")")
        }
        is HA_Type.Dto, is HA_Type.Ref -> {
            add("%T(", objectTypeType.importNested())
            appendStructure(notNullType, model)
            add(")")
        }
        is HA_Type.UrlParam -> {
            add("%T(", objectTypeType.importNested())
            appendStructure(notNullType, model)
            add(")")
        }
        is HA_Type.Object -> when (notNullType.kind) {
            PAIR, TRIPLE, MOD -> {
                add("%T(", objectTypeType.importNested())
                appendStructure(notNullType, model)
                add(")")
            }
            BATCH -> {
                add("%T(", batchTypeType.importNested())
                appendType(notNullType.batchDataElementType(), model, false)
                add(")")
            }
            SYNC_BATCH -> {
                add("%T(", syncBatchTypeType.importNested())
                appendType(notNullType.batchDataElementType(), model, false)
                add(")")
            }
            REQUEST_BODY -> error("Request bodies cannot appear in parameters")
        }
        is HA_Type.Enum -> {
            add("%T<%T>()", enumTypeType.importNested(), notNullType.kotlinPoet(model))
        }
    }.let {}
    if (type.nullable) add(")")
    if (option) add(")")

    return this
}
