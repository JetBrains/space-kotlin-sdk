package space.jetbrains.api.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import space.jetbrains.api.generator.FieldState.*
import space.jetbrains.api.generator.HierarchyRole2.*
import java.util.TreeMap


inline fun <T> dfs(root: T, crossinline getChildren: (T) -> Iterable<T>): Sequence<T> = sequence {
    val stack = mutableListOf(root)
    while (stack.isNotEmpty()) {
        val node = stack.removeAt(stack.lastIndex)
        yield(node)
        stack.addAll(getChildren(node))
    }
}

private val propertyValueType = ClassName(ROOT_PACKAGE, "PropertyValue")
private val propertyValueValueType = propertyValueType.nestedClass("Value")
val HA_Dto.isObject get() = hierarchyRole2 == FINAL_CLASS && fields.isEmpty()

fun generateTypes(model: HttpApiEntitiesById): List<FileSpec> {
    val fieldDescriptorsByDtoId = model.buildFieldsByDtoId()

    Log.info { "Generating DTO classes for SDK" }
    return model.dtoAndUrlParams.values.mapNotNull { root ->
        if (root.extends != null) return@mapNotNull null

        val rootClassName = root.getClassName()
        if (rootClassName == batchInfoType) return@mapNotNull null

        Log.info { "Generating file with DTO classes for '${root.name}' and its subclasses" }
        FileSpec.builder(rootClassName.packageName, rootClassName.simpleNames.joinToString("")).also { fileBuilder ->
            fileBuilder.indent(INDENT)

            fileBuilder.addImport(ROOT_PACKAGE, "getValue")

            root.topLevelSubclasses(model).forEach { dto ->
                fileBuilder.addType(dtoDeclaration(dto, model, fieldDescriptorsByDtoId))
            }
        }.build()
    } + model.enums.values.map { enumType ->
        val enumClassName = enumType.name.kotlinClassNameJoined()

        FileSpec.builder(TYPES_PACKAGE, enumClassName).apply {
            addType(TypeSpec.enumBuilder(enumClassName).apply {
                enumType.values.forEach {
                    addEnumConstant(it)
                }
                annotationSpecs.deprecation(enumType.deprecation)
            }.build())
        }.build()
    }
}

private fun dtoDeclaration(dto: HA_Dto, model: HttpApiEntitiesById, fieldDescriptorsByDtoId: Map<TID, List<FieldDescriptor>>): TypeSpec {
    Log.info { "Generating DTO class for '${dto.name}'" }
    val dtoClassName = dto.getClassName()
    val typeBuilder = when (dto.hierarchyRole2) {
        SEALED_CLASS -> TypeSpec.classBuilder(dtoClassName).addModifiers(KModifier.SEALED)
        OPEN_CLASS -> TypeSpec.classBuilder(dtoClassName).addModifiers(KModifier.OPEN)
        FINAL_CLASS -> if (dto.isObject) TypeSpec.objectBuilder(dtoClassName) else TypeSpec.classBuilder(dtoClassName)
        ABSTRACT_CLASS -> TypeSpec.classBuilder(dtoClassName).addModifiers(KModifier.ABSTRACT)
        INTERFACE -> TypeSpec.interfaceBuilder(dtoClassName)
        SEALED_INTERFACE -> TypeSpec.interfaceBuilder(dtoClassName).addModifiers(KModifier.SEALED)
    }

    dto.superclass(model)?.let {
        typeBuilder.superclass(it.getClassName())
    }

    typeBuilder.addSuperinterfaces(dto.implements.map {
        model.resolveDto(it).getClassName()
    })

    val primaryConstructor = FunSpec.constructorBuilder()
    val secondaryConstructor = FunSpec.constructorBuilder()
    val constructorArgs = mutableListOf<CodeBlock>()
    val superclassConstructorArgs = TreeMap<Int, String>()

    val fields = fieldDescriptorsByDtoId.getValue(dto.id)
    fields.forEach {
        val kotlinPoetType = it.field.type.kotlinPoet(model, it.field.requiresOption)
        primaryConstructor
            .addParameter(it.field.name, propertyValueType.parameterizedBy(kotlinPoetType))
        secondaryConstructor
            .addParameter(it.field.name, kotlinPoetType)
        constructorArgs += CodeBlock.of("%T(${it.field.name})", propertyValueValueType)

        when (val fieldsState = it.state) {
            OwnFinal -> {
                typeBuilder.addProperty(
                    PropertySpec.builder(it.field.name, kotlinPoetType)
                        .delegate(it.field.name)
                        .apply { annotations.deprecation(it.field.deprecation) }
                        .build()
                )

            }
            OwnOpen -> {
                typeBuilder.addProperty(
                    PropertySpec.builder(it.field.name, kotlinPoetType)
                        .delegate(it.field.name)
                        .addModifiers(KModifier.OPEN)
                        .apply { annotations.deprecation(it.field.deprecation) }
                        .build()
                )
            }
            is Inherited -> {
                superclassConstructorArgs[fieldsState.field.index] = it.field.name
                null
            }
            is Overrides -> {
                superclassConstructorArgs[fieldsState.field.index] = it.field.name
                typeBuilder.addProperty(
                    PropertySpec.builder(it.field.name, kotlinPoetType)
                        .delegate(it.field.name)
                        .addModifiers(OVERRIDE)
                        .apply { annotations.deprecation(it.field.deprecation) }
                        .build()
                )
            }
        }.let {}
    }
    superclassConstructorArgs.values.forEach { typeBuilder.addSuperclassConstructorParameter(it) }

    if (!dto.hierarchyRole2.isInterface && !dto.isObject) {
        typeBuilder.primaryConstructor(primaryConstructor.build())

        if (fields.isNotEmpty()) {
            typeBuilder.addFunction(
                secondaryConstructor
                    .callThisConstructor(constructorArgs)
                    .build()
            )
        }
    }

    typeBuilder.annotationSpecs.deprecation(dto.deprecation)

    dto.directlyNestedClasses(model).forEach {
        typeBuilder.addType(dtoDeclaration(it, model, fieldDescriptorsByDtoId))
    }

    if (dto.id in model.urlParams) {
        typeBuilder.addProperty(
            PropertySpec.builder("compactId", STRING)
                .getter(
                    FunSpec.getterBuilder()
                        .addCode(CodeBlock.builder().also {
                            it.add("return ")
                            urlParamToString(
                                model = model,
                                expr = "this",
                                type = HA_Type.UrlParam(HA_UrlParameter.Ref(dto.id), nullable = false),
                                funcCode = it
                            )
                        }.build())
                        .build()
                )
                .build()
        )
    }

    return typeBuilder.build()
}

fun HA_Dto.subclasses(model: HttpApiEntitiesById): Sequence<HA_Dto> {
    return if (!hierarchyRole2.isInterface) {
        dfs(this) { it.inheritors.asSequence().map(model::resolveDto).asIterable() }
    } else sequenceOf(this)
}

fun HA_Dto.directlyNestedClasses(model: HttpApiEntitiesById): Sequence<HA_Dto> {
    return model.dtoAndUrlParams.values.asSequence().filter {
        it.name.startsWith("$name.") && '.' !in it.name.removePrefix("$name.")
    }
}

fun HA_Dto.topLevelSubclasses(model: HttpApiEntitiesById): Sequence<HA_Dto> {
    return subclasses(model).filter { '.' !in it.name }
}

fun HttpApiEntitiesById.buildFieldsByDtoId(): Map<TID, List<FieldDescriptor>> {
    Log.info { "Generating field descriptors for DTOs" }
    val result: MutableMap<TID, MutableList<FieldDescriptor>> = mutableMapOf()

    dtoAndUrlParams.values.asSequence()
        .filter { it.extends == null }
        .flatMap { it.subclasses(this) }
        .forEach { dto ->
            val parentFields = dto.extends?.id?.let { result.getValue(it) }
            val fields = result.computeIfAbsent(dto.id) { mutableListOf() }

            dto.fields.forEachIndexed { index, dtoField ->
                fields += parentFields?.find { it.field.name == dtoField.field.name }?.override(dtoField, index)
                    ?: FieldDescriptor(dtoField, index, OwnFinal, dtoField.extension)
            }

            parentFields?.forEachIndexed { parentIndex, parentField ->
                if (fields.none { it.field.name == parentField.field.name }) {
                    fields += FieldDescriptor(parentField.field, parentIndex, Inherited(parentField), parentField.isExtension)
                }
            }
        }

    return result
}

class FieldDescriptor(val field: HA_DtoField, val index: Int, var state: FieldState, val isExtension: Boolean) {
    fun override(field: HA_DtoField, index: Int): FieldDescriptor = when (val state = state) {
        OwnFinal -> {
            this.state = OwnOpen
            FieldDescriptor(field, index, Overrides(this), false)
        }
        OwnOpen -> FieldDescriptor(field, index, Overrides(this), false)
        is Inherited -> state.field.override(field, index)
        is Overrides -> state.field.override(field, index)
    }
}

sealed class FieldState {
    object OwnFinal : FieldState()
    object OwnOpen : FieldState()
    class Inherited(val field: FieldDescriptor) : FieldState()
    class Overrides(val field: FieldDescriptor) : FieldState()
}

fun HA_Dto.superclass(model: HttpApiEntitiesById): HA_Dto? = extends?.let { model.dtoAndUrlParams.getValue(it.id) }
