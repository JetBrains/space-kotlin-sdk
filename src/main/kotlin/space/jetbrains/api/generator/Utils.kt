package space.jetbrains.api.generator

import space.jetbrains.api.generator.HA_Type.Object.Kind.MAP_ENTRY
import space.jetbrains.api.generator.HA_Type.Object.Kind.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

const val ROOT_PACKAGE = "space.jetbrains.api.runtime"
const val TYPES_PACKAGE = "$ROOT_PACKAGE.types"
const val RESOURCES_PACKAGE = "$ROOT_PACKAGE.resources"
const val STRUCTURES_PACKAGE = "$TYPES_PACKAGE.structure"

private val sDateType = ClassName(ROOT_PACKAGE, "SDate")
private val sDateTimeType = ClassName(ROOT_PACKAGE, "SDateTime")
val batchType = ClassName(ROOT_PACKAGE, "Batch")
val batchInfoType = ClassName(ROOT_PACKAGE, "BatchInfo")
val batchInfoStructureType = ClassName(ROOT_PACKAGE, "BatchInfoStructure")

val apiPairType = ClassName(ROOT_PACKAGE, "ApiPair")
val apiPairStructureType = ClassName(ROOT_PACKAGE, "ApiPairStructure")
val apiTripleType = ClassName(ROOT_PACKAGE, "ApiTriple")
val apiTripleStructureType = ClassName(ROOT_PACKAGE, "ApiTripleStructure")
val apiMapEntryType = ClassName(ROOT_PACKAGE, "ApiMapEntry")
val apiMapEntryStructureType = ClassName(ROOT_PACKAGE, "ApiMapEntryStructure")
val modType = ClassName(ROOT_PACKAGE, "Mod")
val modStructureType = ClassName(ROOT_PACKAGE, "ModStructure")
val optionType = ClassName(ROOT_PACKAGE, "Option")
val optionNoneType = optionType.nestedClass("None")

val deserializationContextType = ClassName(ROOT_PACKAGE, "DeserializationContext")
val partialType = ClassName(ROOT_PACKAGE, "Partial")
val partialSpecialType = partialType.nestedClass("Special")
val jsonValueType = ClassName(ROOT_PACKAGE, "JsonValue")
val jsonObjectFunction = MemberName(ROOT_PACKAGE, "jsonObject")

val typeStructureType = ClassName(ROOT_PACKAGE, "TypeStructure")
val restResourceType = ClassName(ROOT_PACKAGE, "RestResource")
val propertyType = typeStructureType.nestedClass("Property")

private val typeType = ClassName(ROOT_PACKAGE, "Type")

private val numberTypeType = typeType.nestedClass("NumberType")
val byteTypeType = numberTypeType.nestedClass("ByteType")
val shortTypeType = numberTypeType.nestedClass("ShortType")
val intTypeType = numberTypeType.nestedClass("IntType")
val longTypeType = numberTypeType.nestedClass("LongType")
val floatTypeType = numberTypeType.nestedClass("FloatType")
val doubleTypeType = numberTypeType.nestedClass("DoubleType")

private val primitiveTypeType = typeType.nestedClass("PrimitiveType")
val booleanTypeType = primitiveTypeType.nestedClass("BooleanType")
val stringTypeType = primitiveTypeType.nestedClass("StringType")
val dateTypeType = primitiveTypeType.nestedClass("DateType")
val dateTimeTypeType = primitiveTypeType.nestedClass("DateTimeType")

val nullableType = typeType.nestedClass("Nullable")
val optionalType = typeType.nestedClass("Optional")
val arrayTypeType = typeType.nestedClass("ArrayType")
val mapTypeType = typeType.nestedClass("MapType")
val objectTypeType = typeType.nestedClass("ObjectType")
val batchTypeType = typeType.nestedClass("BatchType")
val enumTypeType = typeType.nestedClass("EnumType")

val clientWithContextType = ClassName(ROOT_PACKAGE, "SpaceHttpClientWithCallContext")

val httpMethodType = ClassName("io.ktor.http", "HttpMethod")

const val INDENT = "    "

enum class SpecialPartial {
    MAP, BATCH
}

fun HA_Type.isMap(): Boolean = this is HA_Type.Array && elementType.let { it is HA_Type.Object && it.kind == MAP_ENTRY }

data class PartialDetectionResult(val partial: HA_Type?, val special: SpecialPartial?, val isRef: Boolean)

// TODO nested partials
fun HA_Type?.partial(): PartialDetectionResult = when (this) {
    is HA_Type.Primitive, is HA_Type.Enum, null -> PartialDetectionResult(null, null, false)
    is HA_Type.Array -> elementType.partial().let {
        it.copy(special = if (isMap()) SpecialPartial.MAP else it.special)
    }
    is HA_Type.Object -> when (kind) {
        PAIR, TRIPLE, MOD -> PartialDetectionResult(this, null, false)
        MAP_ENTRY -> valueType().partial()
        BATCH -> batchDataType().partial().copy(special = SpecialPartial.BATCH)
        REQUEST_BODY -> error("Objects of kind ${REQUEST_BODY.name} should not appear in output types")
    }
    is HA_Type.Dto -> PartialDetectionResult(this, null, false)
    is HA_Type.Ref -> PartialDetectionResult(this, null, true)
}.let { it.copy(partial = it.partial?.copy(nullable = false, optional = false)) }

fun HA_Type.kotlinPoet(model: HttpApiEntitiesById): TypeName = when (this) {
    is HA_Type.Primitive -> when (this.primitive) {
        HA_Primitive.Byte -> Byte::class.asClassName()
        HA_Primitive.Short -> Short::class.asClassName()
        HA_Primitive.Int -> Int::class.asClassName()
        HA_Primitive.Long -> Long::class.asClassName()
        HA_Primitive.Float -> Float::class.asClassName()
        HA_Primitive.Double -> Double::class.asClassName()
        HA_Primitive.Boolean -> Boolean::class.asClassName()
        HA_Primitive.String -> String::class.asClassName()
        HA_Primitive.Date -> sDateType
        HA_Primitive.DateTime -> sDateTimeType
    }

    is HA_Type.Array -> {
        val elementType = elementType
        if ((elementType as? HA_Type.Object)?.kind == MAP_ENTRY) {
            Map::class.asClassName().parameterizedBy(
                elementType.keyType().kotlinPoet(model),
                elementType.valueType().kotlinPoet(model)
            )
        }
        else List::class.asClassName().parameterizedBy(elementType.kotlinPoet(model))
    }
    is HA_Type.Object -> when (kind) {
        PAIR -> apiPairType.parameterizedBy(firstType().kotlinPoet(model), secondType().kotlinPoet(model))
        TRIPLE -> apiTripleType.parameterizedBy(
            firstType().kotlinPoet(model),
            secondType().kotlinPoet(model),
            thirdType().kotlinPoet(model)
        )
        MAP_ENTRY -> apiMapEntryType.parameterizedBy(keyType().kotlinPoet(model), valueType().kotlinPoet(model))
        BATCH -> batchType.parameterizedBy(batchDataType().kotlinPoet(model))
        MOD -> modType.parameterizedBy(modSubjectType().kotlinPoet(model))
        REQUEST_BODY -> error("Request bodies are not representable with kotlin types")
    }
    is HA_Type.Dto -> model.dto.getValue(dto.id).getClassName()
    is HA_Type.Ref -> model.dto.getValue(dto.id).getClassName()
    is HA_Type.Enum -> ClassName(TYPES_PACKAGE, model.enums.getValue(enum.id).name.kotlinClassName())
}.copy(nullable, optional)

private fun TypeName.copy(nullable: Boolean, optional: Boolean): TypeName {
    val type = if (isNullable != nullable) copy(nullable) else this

    return if (optional) {
        optionType.parameterizedBy(type)
    }
    else type
}

fun MutableList<AnnotationSpec>.deprecation(deprecation: HA_Deprecation?) {
    if (deprecation != null) {
        val message = deprecation.message + ", since " + deprecation.since + if (deprecation.forRemoval)
            ", WILL BE REMOVED"
        else ""
        this += AnnotationSpec.builder(Deprecated::class).addMember("%S", message).build()
    }
}

fun String.displayNameToClassName(): String = splitToSequence(' ')
    .map { it.first().toUpperCase() + it.drop(1).toLowerCase() }
    .joinToString("") { it.filter(Char::isJavaIdentifierPart) }
    .dropWhile { !it.isJavaIdentifierStart() }
    .let { it.first().toUpperCase() + it.drop(1) }

private fun String.classNameToMemberName() = first().toLowerCase() + drop(1)
fun String.displayNameToMemberName(): String = displayNameToClassName().classNameToMemberName()
fun String.kotlinClassName() = replace(".", "")

private fun HA_Type.Object.fieldTypeByName(name: String) = fields.first { it.name == name }.type
fun HA_Type.Object.firstType() = fieldTypeByName("first")
fun HA_Type.Object.secondType() = fieldTypeByName("second")
fun HA_Type.Object.thirdType() = fieldTypeByName("third")
fun HA_Type.Object.modSubjectType() = fieldTypeByName("old").copy(nullable = false)
fun HA_Type.Object.keyType() = fieldTypeByName("key")
fun HA_Type.Object.valueType() = fieldTypeByName("value")
fun HA_Type.Object.batchDataType() = (fieldTypeByName("data") as HA_Type.Array).elementType

fun ClassName.getStructureClassName() = if (this != batchInfoType) {
    ClassName(STRUCTURES_PACKAGE, "${simpleName.kotlinClassName()}Structure")
}
else batchInfoStructureType

fun HA_Dto.getClassName() = if (name != "BatchInfo") {
    ClassName(TYPES_PACKAGE, name.kotlinClassName())
}
else batchInfoType

fun HttpApiEntitiesById.resolveDto(dto: HA_Dto.Ref): HA_Dto = this.dto.getValue(dto.id)
