package space.jetbrains.api.generator

import space.jetbrains.api.generator.HA_Type.Object.Kind.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

const val ROOT_PACKAGE = "space.jetbrains.api.runtime"
const val TYPES_PACKAGE = "$ROOT_PACKAGE.types"
const val RESOURCES_PACKAGE = "$ROOT_PACKAGE.resources"
const val STRUCTURES_PACKAGE = "$TYPES_PACKAGE.structure"
const val PARTIALS_PACKAGE = "$TYPES_PACKAGE.partials"
const val MENU_PACKAGE = "$ROOT_PACKAGE.menu"

private val localDateType = ClassName("kotlinx.datetime", "LocalDate")
private val instantType = ClassName("kotlinx.datetime", "Instant")
val batchType = ClassName(ROOT_PACKAGE, "Batch")
val batchInfoType = ClassName(ROOT_PACKAGE, "BatchInfo")
val batchInfoStructureType = ClassName(ROOT_PACKAGE, "BatchInfoStructure")

val apiPairType = ClassName(ROOT_PACKAGE, "ApiPair")
val apiPairStructureType = ClassName(ROOT_PACKAGE, "ApiPairStructure")
val apiPairPartialType = ClassName(ROOT_PACKAGE, "ApiPairPartial")

val apiTripleType = ClassName(ROOT_PACKAGE, "ApiTriple")
val apiTripleStructureType = ClassName(ROOT_PACKAGE, "ApiTripleStructure")
val apiTriplePartialType = ClassName(ROOT_PACKAGE, "ApiTriplePartial")

val modType = ClassName(ROOT_PACKAGE, "Mod")
val modStructureType = ClassName(ROOT_PACKAGE, "ModStructure")
val modPartialType = ClassName(ROOT_PACKAGE, "ModPartial")

val optionType = ClassName(ROOT_PACKAGE, "Option")
val optionNoneType = optionType.nestedClass("None")

val deserializationContextType = ClassName(ROOT_PACKAGE, "DeserializationContext")
val partialBuilderType = ClassName(ROOT_PACKAGE, "PartialBuilder", "Explicit")

val partialType = ClassName(ROOT_PACKAGE, "Partial")
val partialImplType = ClassName(ROOT_PACKAGE, "PartialImpl")

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

val clientType = ClassName(ROOT_PACKAGE, "SpaceClient")

val httpMethodType = ClassName("io.ktor.http", "HttpMethod")
val parametersType = ClassName("io.ktor.http", "Parameters")

const val INDENT = "    "

data class PartialDetectionResult(val partial: HA_Type?, val batch: Boolean)

// TODO nested partials
fun HA_Type?.partial(): PartialDetectionResult = when (this) {
    is HA_Type.Primitive, is HA_Type.Enum, null -> PartialDetectionResult(null, false)
    is HA_Type.Array -> elementType.partial()
    is HA_Type.Map -> valueType.partial()
    is HA_Type.Object -> when (kind) {
        PAIR, TRIPLE, MOD -> PartialDetectionResult(this, false)
        BATCH -> batchDataElementType().partial().copy(batch = true)
        REQUEST_BODY -> error("Objects of kind ${REQUEST_BODY.name} should not appear in output types")
    }
    is HA_Type.UrlParam -> PartialDetectionResult(this, false)
    is HA_Type.Dto -> PartialDetectionResult(this, false)
    is HA_Type.Ref -> PartialDetectionResult(this, false)
}.let { it.copy(partial = it.partial?.copy(nullable = false)) }

fun HA_Type.kotlinPoet(model: HttpApiEntitiesById, option: Boolean = false): TypeName = when (this) {
    is HA_Type.Primitive -> when (this.primitive) {
        HA_Primitive.Byte -> Byte::class.asClassName()
        HA_Primitive.Short -> Short::class.asClassName()
        HA_Primitive.Int -> Int::class.asClassName()
        HA_Primitive.Long -> Long::class.asClassName()
        HA_Primitive.Float -> Float::class.asClassName()
        HA_Primitive.Double -> Double::class.asClassName()
        HA_Primitive.Boolean -> Boolean::class.asClassName()
        HA_Primitive.String -> String::class.asClassName()
        HA_Primitive.Date -> localDateType
        HA_Primitive.DateTime -> instantType
    }

    is HA_Type.Array -> List::class.asClassName().parameterizedBy(elementType.kotlinPoet(model))
    is HA_Type.Map -> Map::class.asClassName().parameterizedBy(STRING, valueType.kotlinPoet(model))

    is HA_Type.Object -> when (kind) {
        PAIR -> apiPairType.parameterizedBy(firstType().kotlinPoet(model), secondType().kotlinPoet(model))
        TRIPLE -> apiTripleType.parameterizedBy(
            firstType().kotlinPoet(model),
            secondType().kotlinPoet(model),
            thirdType().kotlinPoet(model)
        )
        BATCH -> batchType.parameterizedBy(batchDataElementType().kotlinPoet(model))
        MOD -> modType.parameterizedBy(modSubjectType().kotlinPoet(model))
        REQUEST_BODY -> error("Request bodies are not representable with kotlin types")
    }
    is HA_Type.Dto -> model.resolveDto(dto).getClassName()
    is HA_Type.Ref -> model.resolveDto(dto).getClassName()
    is HA_Type.Enum -> ClassName(TYPES_PACKAGE, model.enums.getValue(enum.id).name.kotlinClassNameJoined())
    is HA_Type.UrlParam -> model.dtoAndUrlParams.getValue(urlParam.id).getClassName() // TODO: Support UrlParam
}.copy(nullable, option)

private fun TypeName.copy(nullable: Boolean, option: Boolean): TypeName {
    val type = if (isNullable != nullable) copy(nullable) else this

    return if (option) {
        optionType.parameterizedBy(type)
    } else type
}

fun MutableList<AnnotationSpec>.deprecation(deprecation: HA_Deprecation?) {
    if (deprecation != null) {
        val message = deprecation.message + ", since " + deprecation.since + if (deprecation.forRemoval)
            ", WILL BE REMOVED"
        else ""
        this += AnnotationSpec.builder(Deprecated::class).addMember("%S", message).build()
    }
}

private inline fun String.splitByPredicate(predicate: (Char) -> Boolean): List<String> {
    val result = mutableListOf<String>()
    var lastIndex = 0
    forEachIndexed { index, c ->
        if (predicate(c)) {
            if (lastIndex < index) {
                result.add(substring(lastIndex, index))
            }
            lastIndex = index + 1
        }
    }
    if (lastIndex != length) result.add(substring(lastIndex))
    return result
}

fun String.displayNameToClassName(): String = lowercase()
    .splitByPredicate { !it.isJavaIdentifierPart() }
    .joinToString("") { it.replaceFirstChar(Char::titlecase) }
    .dropWhile { !it.isJavaIdentifierStart() }
    .replaceFirstChar(Char::titlecase)

fun String.displayNameToMemberName(): String = displayNameToClassName().replaceFirstChar(Char::lowercase)
fun String.kotlinClassNameJoined(): String = replace(".", "")
fun String.kotlinClassName(): List<String> = split(".")

private fun HA_Type.Object.fieldByName(name: String) = fields.first { it.name == name }

fun HA_Type.Object.firstField() = fieldByName("first")
fun HA_Type.Object.firstType() = firstField().type

fun HA_Type.Object.secondField() = fieldByName("second")
fun HA_Type.Object.secondType() = secondField().type

fun HA_Type.Object.thirdField() = fieldByName("third")
fun HA_Type.Object.thirdType() = thirdField().type

fun HA_Type.Object.modSubjectType() = fieldByName("old").type.copy(nullable = false)

fun HA_Type.Object.batchDataField() = fieldByName("data")
fun HA_Type.Object.batchDataElementType() = (batchDataField().type as HA_Type.Array).elementType

fun ClassName.getStructureClassName() = if (this != batchInfoType) {
    ClassName(STRUCTURES_PACKAGE, simpleNames.joinToString("") + "Structure")
} else batchInfoStructureType

fun ClassName.dtoToPartialInterface() = ClassName(PARTIALS_PACKAGE, simpleNames.joinToString("") + "Partial")
fun ClassName.dtoToPartialImpl() = ClassName(PARTIALS_PACKAGE, simpleNames.joinToString("") + "PartialImpl")
fun ClassName.partialInterfaceToImpl() = ClassName(packageName, "${simpleName}Impl")

fun HA_Dto.getClassName() = if (name != "BatchInfo") {
    ClassName(TYPES_PACKAGE, name.kotlinClassName())
} else batchInfoType

fun HA_UrlParameterOption.getClassName() = ClassName(TYPES_PACKAGE, optionName.kotlinClassName())

fun HttpApiEntitiesById.resolveDto(dto: HA_Dto.Ref): HA_Dto = this.dtoAndUrlParams.getValue(dto.id)
fun HttpApiEntitiesById.resolveDto(type: HA_Type.Dto): HA_Dto = resolveDto(type.dto)
fun HttpApiEntitiesById.resolveDto(type: HA_Type.Ref): HA_Dto = resolveDto(type.dto)
fun HttpApiEntitiesById.resolveUrlParam(type: HA_Type.UrlParam): HA_Dto = this.dtoAndUrlParams.getValue(type.urlParam.id)

fun HA_Description.buildKDoc() = buildString {
    appendKDoc(this@buildKDoc)
}

val HA_Description.helpTopicLink
    get() = helpTopic?.let { "https://www.jetbrains.com/help/space/$it" }

fun StringBuilder.appendKDoc(description: HA_Description) {
    appendLine(description.text)
    description.helpTopicLink?.let {
        appendLine()
        appendLine("[Read more]($it)")
    }
}