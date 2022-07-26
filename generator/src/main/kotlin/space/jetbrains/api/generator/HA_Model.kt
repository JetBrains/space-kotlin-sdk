@file:Suppress("ClassName")

package space.jetbrains.api.generator

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo

typealias TID = String

class HA_Model(
    val dto: List<HA_Dto>,
    val enums: List<HA_Enum>,
    val urlParams: List<HA_UrlParameter>,
    val resources: List<HA_Resource>,
    val menuIds: List<HA_MenuId>,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "className")
@JsonSubTypes(
    Type(value = HA_PathSegment.Var::class, name = "HA_PathSegment.Var"),
    Type(value = HA_PathSegment.PrefixedVar::class, name = "HA_PathSegment.PrefixedVar"),
    Type(value = HA_PathSegment.Const::class, name = "HA_PathSegment.Const")
)
sealed class HA_PathSegment {
    data class Var(val name: String) : HA_PathSegment()
    data class PrefixedVar(val prefix: String, val name: String) : HA_PathSegment()
    data class Const(val value: String) : HA_PathSegment()
}

data class HA_Path(val segments: List<HA_PathSegment>)

class HA_Resource(
    val id: TID,
    val path: HA_Path,
    val displaySingular: String,
    val displayPlural: String,
    val parentResource: Ref?,
    val nestedResources: List<HA_Resource>,
    val endpoints: List<HA_Endpoint>
) {
    class Ref(val id: TID)
}

enum class HA_Method(val methodName: String, val hasBody: Boolean) {
    REST_CREATE("POST", hasBody = true),
    REST_QUERY("GET", hasBody = false),
    REST_GET("GET", hasBody = false),
    REST_UPDATE("PATCH", hasBody = true),
    REST_DELETE("DELETE", hasBody = false),

    HTTP_GET("GET", hasBody = false),
    HTTP_POST("POST", hasBody = true),
    HTTP_PATCH("PATCH", hasBody = true),
    HTTP_PUT("PUT", hasBody = true),
    HTTP_DELETE("DELETE", hasBody = false)
}

class HA_Endpoint(
    val resource: HA_Resource.Ref,
    val method: HA_Method,
    val parameters: List<HA_Parameter>,
    val requestBody: HA_Type.Object?,
    val responseBody: HA_Type?,
    val path: HA_Path,
    val displayName: String,
    val functionName: String,
    val description: HA_Description? = null,
    val deprecation: HA_Deprecation? = null,
    val experimental: HA_Experimental? = null
)

class HA_Parameter(
    val field: HA_Field,
    val path: Boolean
)

data class HA_Description(
    val text: String,
    val helpTopic: String?
)

data class HA_Deprecation(
    val message: String,
    val since: String,
    val forRemoval: Boolean
)

data class HA_Experimental(val message: String?)


enum class HA_Primitive(val presentation: kotlin.String) {
    Byte("byte"),
    Short("short"),
    Int("int"),
    Long("long"),
    Float("float"),
    Double("double"),
    Boolean("boolean"),
    String("string"),
    Date("date"),
    DateTime("datetime"),
    Duration("duration")
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "className")
@JsonSubTypes(
    Type(value = HA_Type.Primitive::class, name = "HA_Type.Primitive"),
    Type(value = HA_Type.Array::class, name = "HA_Type.Array"),
    Type(value = HA_Type.Map::class, name = "HA_Type.Map"),
    Type(value = HA_Type.Object::class, name = "HA_Type.Object"),
    Type(value = HA_Type.Dto::class, name = "HA_Type.Dto"),
    Type(value = HA_Type.Ref::class, name = "HA_Type.Ref"),
    Type(value = HA_Type.Enum::class, name = "HA_Type.Enum"),
    Type(value = HA_Type.UrlParam::class, name = "HA_Type.UrlParam")
)
sealed class HA_Type {
    abstract val nullable: Boolean

    data class Primitive(val primitive: HA_Primitive, override val nullable: Boolean) : HA_Type()
    data class Array(val elementType: HA_Type, override val nullable: Boolean) : HA_Type()
    data class Map(val valueType: HA_Type, override val nullable: Boolean) : HA_Type()

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    data class Object(val fields: List<HA_Field>, val kind: Kind, override val nullable: Boolean) : HA_Type() {
        enum class Kind(val isBatch: Boolean = false) {
            PAIR, TRIPLE, BATCH(true), SYNC_BATCH(true), MOD, REQUEST_BODY
        }
    }
    data class Dto(val dto: HA_Dto.Ref, override val nullable: Boolean) : HA_Type()
    data class Ref(val dto: HA_Dto.Ref, override val nullable: Boolean) : HA_Type()
    data class Enum(val enum: HA_Enum.Ref, override val nullable: Boolean) : HA_Type()
    data class UrlParam(val urlParam: HA_UrlParameter.Ref, override val nullable: Boolean) : HA_Type()

    fun copy(nullable: Boolean): HA_Type = when (this) {
        is Primitive -> Primitive(primitive, nullable)
        is Array -> Array(elementType, nullable)
        is Map -> Map(valueType, nullable)
        is Object -> Object(fields, kind, nullable)
        is Dto -> Dto(dto, nullable)
        is Ref -> Ref(dto, nullable)
        is Enum -> Enum(enum, nullable)
        is UrlParam -> UrlParam(urlParam, nullable)
    }
}

data class HA_Field(
    val name: String,
    val type: HA_Type,
    val description: HA_Description?,
    val deprecation: HA_Deprecation?,
    val experimental: HA_Experimental?,
    val optional: Boolean,
    val defaultValue: HA_DefaultValue?
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "className")
@JsonSubTypes(
    Type(value = HA_DefaultValue.Const.Primitive::class, name = "HA_DefaultValue.Const.Primitive"),
    Type(value = HA_DefaultValue.Const.EnumEntry::class, name = "HA_DefaultValue.Const.EnumEntry"),
    Type(value = HA_DefaultValue.Collection::class, name = "HA_DefaultValue.Collection"),
    Type(value = HA_DefaultValue.Reference::class, name = "HA_DefaultValue.Reference")
)
sealed class HA_DefaultValue {
    sealed class Const : HA_DefaultValue() {
        data class Primitive(val expression: String) : Const()
        data class EnumEntry(val entryName: String) : Const()
    }
    data class Collection(val elements: List<HA_DefaultValue>) : HA_DefaultValue()
    data class Map(val elements: kotlin.collections.Map<String, HA_DefaultValue>) : HA_DefaultValue()
    data class Reference(val paramName: String): HA_DefaultValue()

    companion object {
        val NULL = Const.Primitive("null")
    }
}

class HA_DtoField(val field: HA_Field, val extension: Boolean)

val HA_DtoField.name get() = field.name
val HA_DtoField.type get() = field.type
val HA_DtoField.description get() = field.description
val HA_DtoField.deprecation get() = field.deprecation
val HA_DtoField.experimental get() = field.experimental
val HA_DtoField.requiresOption get() = field.requiresOption

class HA_Dto(
    val id: TID,
    val name: String,
    val fields: List<HA_DtoField>,
    val hierarchyRole2: HierarchyRole2,
    val extends: Ref?,
    val implements: List<Ref>,
    val inheritors: List<Ref>,
    val description: HA_Description?,
    val deprecation: HA_Deprecation?,
    val experimental: HA_Experimental?,
    val record: Boolean
) {
    data class Ref(val id: TID)
}

enum class HierarchyRole2(val isAbstract: Boolean, val isInterface: Boolean) {
    SEALED_CLASS(isAbstract = true, isInterface = false),
    OPEN_CLASS(isAbstract = false, isInterface = false),
    FINAL_CLASS(isAbstract = false, isInterface = false),
    ABSTRACT_CLASS(isAbstract = true, isInterface = false),
    INTERFACE(isAbstract = true, isInterface = true),
    SEALED_INTERFACE(isAbstract = true, isInterface = true),
}

class HA_Enum(
    val id: TID,
    val name: String,
    val values: List<String>,
    val deprecation: HA_Deprecation?,
    val experimental: HA_Experimental?
) {
    class Ref(val id: TID)
}

class HA_UrlParameter(
    val id: TID,
    val name: String,
    val options: List<HA_UrlParameterOption>,
    val deprecation: HA_Deprecation?,
    val experimental: HA_Experimental?
) {
    class Ref(val id: TID)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "className")
@JsonSubTypes(
    Type(value = HA_UrlParameterOption.Const::class, name = "HA_UrlParameterOption.Const"),
    Type(value = HA_UrlParameterOption.Var::class, name = "HA_UrlParameterOption.Var")
)
sealed class HA_UrlParameterOption {
    abstract val optionName: String
    abstract val description: HA_Description?
    abstract val deprecation: HA_Deprecation?
    abstract val experimental: HA_Experimental?

    class Const(
        val value: String,
        override val optionName: String,
        override val description: HA_Description?,
        override val deprecation: HA_Deprecation?,
        override val experimental: HA_Experimental?
    ) : HA_UrlParameterOption()

    class Var(
        val parameters: List<HA_Field>,
        val prefixRequired: Boolean,
        override val optionName: String,
        override val description: HA_Description?,
        override val deprecation: HA_Deprecation?,
        override val experimental: HA_Experimental?
    ) : HA_UrlParameterOption()
}

data class HA_MenuId(val menuId: String, val context: HA_Dto.Ref)
