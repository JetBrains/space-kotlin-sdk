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
    val resources: List<HA_Resource>
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
    val doc: String?,
    val deprecation: HA_Deprecation? = null
)

class HA_Parameter(
    val field: HA_Field,
    val path: Boolean
)

data class HA_Deprecation(
    val message: String,
    val since: String,
    val forRemoval: Boolean
)


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
    DateTime("datetime")
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "className")
@JsonSubTypes(
    Type(value = HA_Type.Primitive::class, name = "HA_Type.Primitive"),
    Type(value = HA_Type.Array::class, name = "HA_Type.Array"),
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    data class Object(val fields: List<HA_Field>, val kind: Kind, override val nullable: Boolean) : HA_Type() {
        enum class Kind {
            PAIR, TRIPLE, MAP_ENTRY, BATCH, MOD, REQUEST_BODY
        }
    }
    data class Dto(val dto: HA_Dto.Ref, override val nullable: Boolean) : HA_Type()
    data class Ref(val dto: HA_Dto.Ref, override val nullable: Boolean) : HA_Type()
    data class Enum(val enum: HA_Enum.Ref, override val nullable: Boolean) : HA_Type()
    data class UrlParam(val urlParam: HA_UrlParameter.Ref, override val nullable: Boolean) : HA_Type()

    fun copy(nullable: Boolean = this.nullable): HA_Type = when (this) {
        is Primitive -> Primitive(primitive, nullable)
        is Array -> Array(elementType, nullable)
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
    val deprecation: HA_Deprecation?,
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
    data class Reference(val paramName: String): HA_DefaultValue()
}

class HA_DtoField(val field: HA_Field, val extension: Boolean)

class HA_Dto(
    val id: TID,
    val name: String,
    val fields: List<HA_DtoField>,
    val hierarchyRole: HierarchyRole,
    val extends: Ref?,
    val implements: List<Ref>,
    val inheritors: List<Ref>,
    val deprecation: HA_Deprecation?,
    val record: Boolean
) {
    data class Ref(val id: TID)
}

enum class HierarchyRole(val isAbstract: Boolean) {
    SEALED(true),
    OPEN(false),
    FINAL(false),
    ABSTRACT(true),
    INTERFACE(true)
}

class HA_Enum(
    val id: TID,
    val name: String,
    val values: List<String>,
    val deprecation: HA_Deprecation?
) {
    class Ref(val id: TID)
}

class HA_UrlParameter(
    val id: TID,
    val name: String,
    val options: List<HA_UrlParameterOption>,
    val deprecation: HA_Deprecation?
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
    abstract val deprecation: HA_Deprecation?

    class Const(
        val value: String,
        override val optionName: String,
        override val deprecation: HA_Deprecation?
    ) : HA_UrlParameterOption()

    class Var(
        val parameter: HA_Field,
        val prefixRequired: Boolean,
        override val optionName: String,
        override val deprecation: HA_Deprecation?
    ) : HA_UrlParameterOption()
}
