package space.jetbrains.api.generator

import space.jetbrains.api.generator.HA_Type.Object.Kind.*
import space.jetbrains.api.generator.HA_Type.Object.Kind.MAP_ENTRY
import com.squareup.kotlinpoet.*

fun structureCode(type: HA_Type, model: HttpApiEntitiesById): Pair<String, Array<ClassName>> {
    val structures = mutableListOf<ClassName>()
    return buildString { appendStructure(type, structures, model) } to structures.toTypedArray()
}

fun StringBuilder.appendStructure(type: HA_Type, structures: MutableList<in ClassName>, model: HttpApiEntitiesById): StringBuilder {
    append("%T")
    when (type) {
        is HA_Type.Primitive -> error("Primitives have no structure")
        is HA_Type.Array -> error("Arrays have no structure")
        is HA_Type.Object -> when (type.kind) {
            PAIR -> {
                structures += apiPairStructureType
                append('(')
                appendType(type.firstType(), structures, model)
                append(", ")
                appendType(type.secondType(), structures, model)
                append(')')
            }
            TRIPLE -> {
                structures += apiTripleStructureType
                append('(')
                appendType(type.firstType(), structures, model)
                append(", ")
                appendType(type.secondType(), structures, model)
                append(", ")
                appendType(type.thirdType(), structures, model)
                append(')')
            }
            MAP_ENTRY -> {
                structures += apiMapEntryStructureType
                append('(')
                appendType(type.keyType(), structures, model)
                append(", ")
                appendType(type.valueType(), structures, model)
                append(')')
            }
            BATCH -> error("Batches have no structure")
            MOD -> {
                structures += modStructureType
                append('(')
                appendType(type.modSubjectType(), structures, model)
                append(')')
            }
            REQUEST_BODY -> error("Request bodies cannot appear in parameters")
        }
        is HA_Type.Dto -> {
            structures += model.dto.getValue(type.dto.id).getClassName().getStructureClassName()
        }
        is HA_Type.Ref -> {
            structures += model.dto.getValue(type.dto.id).getClassName().getStructureClassName()
        }
        is HA_Type.Enum -> error("Enums have no structure")
    }

    return this
}

val necessaryImports = listOf(
    optionalType,
    nullableType,

    byteTypeType,
    shortTypeType,
    intTypeType,
    longTypeType,
    floatTypeType,
    doubleTypeType,
    booleanTypeType,
    stringTypeType,
    dateTypeType,
    dateTimeTypeType,

    mapTypeType,
    arrayTypeType,
    batchTypeType,

    objectTypeType,
    enumTypeType
)

fun StringBuilder.appendType(type: HA_Type, types: MutableList<in ClassName>, model: HttpApiEntitiesById): StringBuilder {
    if (type.optional) append(optionalType.simpleName, "(")
    if (type.nullable) append(nullableType.simpleName, "(")
    when (val notNullType = type.copy(nullable = false, optional = false)) {
        is HA_Type.Primitive -> {
            append(
                when (notNullType.primitive) {
                    HA_Primitive.Byte -> byteTypeType
                    HA_Primitive.Short -> shortTypeType
                    HA_Primitive.Int -> intTypeType
                    HA_Primitive.Long -> longTypeType
                    HA_Primitive.Float -> floatTypeType
                    HA_Primitive.Double -> doubleTypeType
                    HA_Primitive.Boolean -> booleanTypeType
                    HA_Primitive.String -> stringTypeType
                    HA_Primitive.Date -> dateTypeType
                    HA_Primitive.DateTime -> dateTimeTypeType
                }.simpleName
            )
        }
        is HA_Type.Array -> {
            val elementType = notNullType.elementType
            if (elementType is HA_Type.Object && elementType.kind == MAP_ENTRY) {
                append(mapTypeType.simpleName, "(")
                appendType(elementType.keyType(), types, model)
                append(", ")
                appendType(elementType.valueType(), types, model)
                append(')')
            } else {
                append(arrayTypeType.simpleName, "(")
                appendType(elementType, types, model)
                append(')')
            }
        }
        is HA_Type.Dto, is HA_Type.Ref -> {
            append(objectTypeType.simpleName, "(")
            appendStructure(notNullType, types, model)
            append(')')
        }
        is HA_Type.Object -> when (notNullType.kind) {
            PAIR, TRIPLE, MAP_ENTRY, MOD -> {
                append(objectTypeType.simpleName, "(")
                appendStructure(notNullType, types, model)
                append(')')
            }
            BATCH -> {
                append(batchTypeType.simpleName, "(")
                appendType(notNullType.batchDataType(), types, model)
                append(')')
            }
            REQUEST_BODY -> error("Request bodies cannot appear in parameters")
        }
        is HA_Type.Enum -> {
            types += notNullType.kotlinPoet(model) as ClassName
            append(enumTypeType.simpleName, "<%T>()")
        }
    }.let {}
    if (type.nullable) append(')')
    if (type.optional) append(')')

    return this
}
