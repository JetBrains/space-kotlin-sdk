package space.jetbrains.api.generator

import space.jetbrains.api.generator.HA_Type.Object.Kind.*
import space.jetbrains.api.generator.HA_Type.Object.Kind.MAP_ENTRY
import com.squareup.kotlinpoet.*

fun CodeBlock.Builder.appendStructure(type: HA_Type, model: HttpApiEntitiesById): CodeBlock.Builder {
    return when (type) {
        is HA_Type.Primitive -> error("Primitives have no structure")
        is HA_Type.Array -> error("Arrays have no structure")
        is HA_Type.Object -> when (type.kind) {
            PAIR -> {
                add("%T(", apiPairStructureType)
                appendType(type.firstType(), model)
                add(", ")
                appendType(type.secondType(), model)
                add(")")
            }
            TRIPLE -> {
                add("%T(", apiTripleStructureType)
                appendType(type.firstType(), model)
                add(", ")
                appendType(type.secondType(), model)
                add(", ")
                appendType(type.thirdType(), model)
                add(")")
            }
            MAP_ENTRY -> {
                add("%T(")
                appendType(type.keyType(), model)
                add(", ")
                appendType(type.valueType(), model)
                add(")")
            }
            BATCH -> error("Batches have no structure")
            MOD -> {
                add("%T(", modStructureType)
                appendType(type.modSubjectType(), model)
                add(")")
            }
            REQUEST_BODY -> error("Request bodies cannot appear in parameters")
        }
        is HA_Type.Dto -> {
            add("%T", model.dtoAndUrlParams.getValue(type.dto.id).getClassName().getStructureClassName())
        }
        is HA_Type.Ref -> {
            add("%T", model.dtoAndUrlParams.getValue(type.dto.id).getClassName().getStructureClassName())
        }
        is HA_Type.Enum -> error("Enums have no structure")
        is HA_Type.UrlParam -> {
            add("%T", model.urlParams.getValue(type.urlParam.id).getClassName().getStructureClassName())
        }
    }
}

fun ClassName.importNested() = ClassName(
    packageName = packageName + "." + simpleNames.dropLast(1).joinToString("."),
    simpleNames = listOf(simpleName)
)

fun CodeBlock.Builder.appendType(
    type: HA_Type,
    model: HttpApiEntitiesById
): CodeBlock.Builder {
    if (type.optional) add("%T(", optionalType.importNested())
    if (type.nullable) add("%T(", nullableType.importNested())
    when (val notNullType = type.copy(nullable = false, optional = false)) {
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
            val elementType = notNullType.elementType
            if (elementType is HA_Type.Object && elementType.kind == MAP_ENTRY) {
                add("%T(", mapTypeType.importNested())
                appendType(elementType.keyType(), model)
                add(", ")
                appendType(elementType.valueType(), model)
                add(")")
            } else {
                add("%T(", arrayTypeType.importNested())
                appendType(elementType, model)
                add(")")
            }
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
            PAIR, TRIPLE, MAP_ENTRY, MOD -> {
                add("%T(", objectTypeType.importNested())
                appendStructure(notNullType, model)
                add(")")
            }
            BATCH -> {
                add("%T(", batchTypeType.importNested())
                appendType(notNullType.batchDataType(), model)
                add(")")
            }
            REQUEST_BODY -> error("Request bodies cannot appear in parameters")
        }
        is HA_Type.Enum -> {
            add("%T<%T>()", enumTypeType.importNested(), notNullType.kotlinPoet(model))
        }
    }.let {}
    if (type.nullable) add(")")
    if (type.optional) add(")")

    return this
}
