package space.jetbrains.api.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

fun generateMenuIds(model: HttpApiEntitiesById): FileSpec {
    return FileSpec.builder(menuIdType.packageName, menuIdType.simpleName).also { file ->
        convert(buildTree("", model.menuIds.map { it.copy(menuId = "." + it.menuId) }), TypeContainer.File(file), model)
    }.build()
}

private val menuIdType = ClassName(MENU_PACKAGE, "MenuId")
private val menuActionContextType = ClassName(TYPES_PACKAGE, "MenuActionContext")

private class Node(val prefix: String, val children: List<Node>, val context: HA_Dto.Ref?)

private fun buildTree(prefix: String, children: List<HA_MenuId>): Node = Node(
    prefix = prefix.removePrefix("."),
    children = children.asSequence().filter {
        it.menuId.startsWith("$prefix.")
    }.groupBy {
        it.menuId.removePrefix("$prefix.").substringBefore('.')
    }.map {
        buildTree("$prefix.${it.key}", it.value)
    },
    context = children.singleOrNull { it.menuId == prefix }?.context
)

private sealed class TypeContainer {
    abstract fun addType(typeSpec: TypeSpec)
    class Type(private val specBuilder: TypeSpec.Builder) : TypeContainer() {
        override fun addType(typeSpec: TypeSpec) {
            specBuilder.addType(typeSpec)
        }
    }
    class File(private val specBuilder: FileSpec.Builder) : TypeContainer() {
        override fun addType(typeSpec: TypeSpec) {
            specBuilder.addType(typeSpec)
        }
    }
}

private fun convert(node: Node, typeContainer: TypeContainer, model: HttpApiEntitiesById) {
    fun TypeSpec.Builder.addChildren() {
        node.children.forEach { convert(it, TypeContainer.Type(this), model) }
    }
    if (node.prefix == "") {
        typeContainer.addType(TypeSpec.classBuilder(menuIdType).also { type ->
            type.addModifiers(SEALED)
            type.addTypeVariable(TypeVariableName("T", menuActionContextType, variance = OUT))
            type.primaryConstructor(FunSpec.constructorBuilder().addParameter("menuId", STRING).build())
            type.addProperty(PropertySpec.builder("menuId", STRING).initializer("menuId").build())
            type.addChildren()
        }.build())
        return
    }
    val name = node.prefix.substringAfterLast(".")
    typeContainer.addType(if (node.context != null) {
        TypeSpec.objectBuilder(name).also { type ->
            type.superclass(menuIdType.parameterizedBy(model.resolveDto(node.context).getClassName()))
            type.addSuperclassConstructorParameter("%S", node.prefix)
            type.addChildren()
        }.build()
    } else {
        TypeSpec.classBuilder(name).also { type ->
            type.primaryConstructor(FunSpec.constructorBuilder().addModifiers(PRIVATE).build())
            type.addChildren()
        }.build()
    })
}
