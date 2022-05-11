package space.jetbrains.api.generator

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

enum class HA_VisibilityModifier {
    DEFAULT,
    PRIVATE,
    INTERNAL
}

class HA_Property(
    val visibilityModifier: HA_VisibilityModifier = HA_VisibilityModifier.DEFAULT,
    val name: String,
    val type: HA_Type,
    val value: HA_DefaultValue?,
    val deprecation: HA_Deprecation?,
    val override: Boolean
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "className")
@JsonSubTypes(
    JsonSubTypes.Type(value = HA_Class.Clazz::class, name = "HA_Class.Clazz"),
    JsonSubTypes.Type(value = HA_Class.Interface::class, name = "HA_Class.Interface"),
    JsonSubTypes.Type(value = HA_Class.Object::class, name = "HA_Class.Object")
)
sealed interface HA_Class {
    val visibilityModifier: HA_VisibilityModifier
    val name: String
    val implements: List<String>
    val innerSubclasses: List<HA_Class>
    val properties: List<HA_Property>
    val deprecation: HA_Deprecation?
    val doc: String?

    class Clazz(
        override val visibilityModifier: HA_VisibilityModifier = HA_VisibilityModifier.DEFAULT,
        override val name: String,
        override val implements: List<String>,
        override val innerSubclasses: List<HA_Class>,
        override val properties: List<HA_Property>,
        override val deprecation: HA_Deprecation?,
        override val doc: String?,
        val open: Boolean,
        val extends: String?,
        val abstract: Boolean,
        val sealed: Boolean,
    ) : HA_Class

    class Interface(
        override val visibilityModifier: HA_VisibilityModifier = HA_VisibilityModifier.DEFAULT,
        override val name: String,
        override val implements: List<String>,
        override val innerSubclasses: List<HA_Class>,
        override val properties: List<HA_Property>,
        override val deprecation: HA_Deprecation?,
        override val doc: String?,
        val sealed: Boolean
    ) : HA_Class

    class Object(
        override val visibilityModifier: HA_VisibilityModifier = HA_VisibilityModifier.DEFAULT,
        override val name: String,
        override val implements: List<String>,
        override val innerSubclasses: List<HA_Class>,
        override val properties: List<HA_Property>,
        override val deprecation: HA_Deprecation?,
        override val doc: String?,
        val extends: String?,
    ) : HA_Class
}
