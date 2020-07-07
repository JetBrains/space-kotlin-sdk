package space.jetbrains.api.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import space.jetbrains.api.generator.HA_Type.Object.Kind.*

fun generatePartials(model: HttpApiEntitiesById): List<FileSpec> {
    val childFieldsById = getChildFields(model)
    val fieldsRequiringJvmName = fieldsRequiringJvmName(model, childFieldsById)

    return model.dtoAndUrlParams.values.mapNotNull { root ->
        if (root.extends != null) return@mapNotNull null

        val rootClassName = root.getClassName()
        if (rootClassName == batchInfoType) return@mapNotNull null

        val rootPartialClassName = rootClassName.dtoToPartialInterface()

        FileSpec.builder(rootPartialClassName.packageName, rootPartialClassName.simpleName).also { file ->
            file.indent(INDENT)
            file.addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE")
                    .addMember("%S", "MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES")
                    .addMember("%S", "INAPPLICABLE_JVM_NAME")
                    .build()
            )

            root.subclasses(model).forEach { dto ->
                val dtoClassName = dto.getClassName()
                val dtoPartialClassName = dtoClassName.dtoToPartialInterface()
                val dtoPartialImplClassName = dtoClassName.dtoToPartialImpl()

                val interfaceBuilder = TypeSpec.interfaceBuilder(dtoPartialClassName)
                    .addSuperinterface(partialType)

                val implBuilder = TypeSpec.classBuilder(dtoPartialImplClassName)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("builder", partialBuilderType)
                            .build()
                    )
                    .superclass(partialImplType)
                    .addSuperclassConstructorParameter("builder")
                    .addSuperinterface(dtoPartialClassName)

                dto.inheritors.forEach {
                    val inheritorPartial = model.resolveDto(it).getClassName().dtoToPartialInterface()
                    val inheritorPartialImpl = inheritorPartial.partialInterfaceToImpl()

                    interfaceBuilder.addSuperinterface(inheritorPartial)
                    implBuilder.addSuperinterface(inheritorPartial, CodeBlock.of("%T(builder)", inheritorPartialImpl))
                }

                val cf = childFieldsById.getValue(dto.id)
                val requiringJvmName = fieldsRequiringJvmName.getValue(dto.id)

                fun FunSpec.Builder.addJvmName(fieldName: String, partialInterface: TypeName) {
                    fun getShortId(typeName: TypeName): String = when (typeName) {
                        is ClassName -> typeName.simpleNames.joinToString("")
                        is ParameterizedTypeName -> getShortId(typeName.rawType) + "_" +
                            typeName.typeArguments.joinToString("_") { getShortId(it) }
                        is WildcardTypeName, is TypeVariableName, is LambdaTypeName, Dynamic -> error("incorrect type")
                    }

                    addAnnotation(
                        AnnotationSpec.builder(JvmName::class)
                            .addMember("%S", fieldName + "_" + getShortId(partialInterface))
                            .build()
                    )
                }

                fun addImplBodyWithPartial(
                    impl: FunSpec.Builder,
                    fieldName: String,
                    buildPartialParameter: ParameterSpec,
                    partialInterface: TypeName,
                    specialPartial: SpecialPartial?
                ) {
                    if (fieldName in requiringJvmName) {
                        impl.addJvmName(fieldName, partialInterface)
                    }

                    impl.addParameter(buildPartialParameter)

                    impl.addCode(buildCodeBlock {
                        add("builder.add(%S, {\n", fieldName)
                        indent()
                        partialImplConstructor(partialInterface, "it")
                        add("\n")
                        unindent()
                        add("}, build")
                        if (specialPartial != null) {
                            file.addImport(partialSpecialType, specialPartial.name)
                            add(", $specialPartial")
                        }
                        add(")")
                    })
                }

                fun addRecursiveAsImpl(fieldName: String, partialInterface: TypeName) {
                    implBuilder.addFunction(
                        FunSpec.builder(fieldName)
                            .addModifiers(OVERRIDE)
                            .addParameter("recursiveAs", partialInterface)
                            .addStatement("builder.addRecursively(%S, getPartialBuilder(recursiveAs))", fieldName)
                            .build()
                    )
                }

                dto.fields.forEach { dtoField ->
                    val field = dtoField.field
                    val (partial, specialPartial) = field.type.partial()

                    val interfaceFunBuilder = FunSpec.builder(field.name).addModifiers(ABSTRACT)
                    val implFunBuilder = FunSpec.builder(field.name).addModifiers(OVERRIDE)

                    val partialInterface = partial?.partialToPartialInterface(model)
                    val isOverride = if (partial in cf.childFieldNamesToPartials[field.name].orEmpty()) {
                        interfaceFunBuilder.addModifiers(OVERRIDE)
                        true
                    } else false

                    if (partial == null) {
                        implFunBuilder.addStatement("builder.add(%S)", field.name)
                    } else {
                        partialInterface!!
                        interfaceBuilder.addFunction(
                            FunSpec.builder(field.name)
                                .addModifiers(ABSTRACT)
                                .addParameter("recursiveAs", partialInterface)
                                .also { if (isOverride) it.addModifiers(OVERRIDE) }
                                .build()
                        )
                        addRecursiveAsImpl(field.name, partialInterface)

                        val buildPartialParameter = ParameterSpec.builder(
                            name = "build",
                            type = LambdaTypeName.get(receiver = partialInterface, returnType = UNIT)
                        ).build()

                        interfaceFunBuilder.addParameter(
                            if (!isOverride) {
                                buildPartialParameter.toBuilder().defaultValue("%T::defaultPartial", partialType).build()
                            } else buildPartialParameter
                        )
                        interfaceFunBuilder.addJvmName(field.name, partialInterface)
                        addImplBodyWithPartial(implFunBuilder, field.name, buildPartialParameter, partialInterface, specialPartial)
                    }

                    interfaceBuilder.addFunction(interfaceFunBuilder.build())
                    implBuilder.addFunction(implFunBuilder.build())
                }

                fun addImpl(fieldName: String, partialInterface: TypeName?, specialPartial: SpecialPartial?) {
                    val implFunBuilder = FunSpec.builder(fieldName).addModifiers(OVERRIDE)

                    if (partialInterface == null) {
                        implFunBuilder.addStatement("builder.add(%S)", fieldName)
                    } else {
                        val buildPartialParameter = ParameterSpec.builder(
                            name = "build",
                            type = LambdaTypeName.get(receiver = partialInterface, returnType = UNIT)
                        ).build()
                        addImplBodyWithPartial(implFunBuilder, fieldName, buildPartialParameter, partialInterface, specialPartial)
                    }

                    implBuilder.addFunction(implFunBuilder.build())
                }

                cf.childFieldNamesToPartials.asSequence()
                    .filter { it.key !in cf.ownFieldNamesToPartials }
                    .flatMap { (fieldName, partials) ->
                        partials.groupingBy { it }
                            .eachCount()
                            .asSequence()
                            .filter { it.value > 1 }
                            .map { fieldName to it.key }
                    }.forEach { (fieldName, partial) ->
                        val partialInterface = partial?.partialToPartialInterface(model)
                        addImpl(fieldName, partialInterface, null)
                        partialInterface?.let { addRecursiveAsImpl(fieldName, it) }
                    }

                if (dto.inheritors.isNotEmpty()) {
                    implBuilder.addFunction(
                        FunSpec.builder("defaultPartial")
                            .addModifiers(OVERRIDE)
                            .addCode("super.defaultPartial()")
                            .build()
                    )
                }

                file.addType(interfaceBuilder.build())
                file.addType(implBuilder.build())
            }
        }.build()
    }
}

fun CodeBlock.Builder.partialImplConstructor(partialInterfaceOrNothing: TypeName, partialBuilderVar: String) {
    return when (partialInterfaceOrNothing) {
        NOTHING -> {
            add("throw %T(%S)", ClassName("kotlin", IllegalArgumentException::class.simpleName!!), "Primitives and enums do not have partials")
        }
        is ClassName -> {
            add("%T($partialBuilderVar)", partialInterfaceOrNothing.partialInterfaceToImpl())
        }

        is ParameterizedTypeName -> {
            val impl = partialInterfaceOrNothing.rawType.partialInterfaceToImpl()
            add("%T(\n", impl.parameterizedBy(partialInterfaceOrNothing.typeArguments))
            indent()
            partialInterfaceOrNothing.typeArguments.forEach {
                add("{ ")
                partialImplConstructor(it, "it")
                add(" },\n")
            }
            add("$partialBuilderVar\n")
            unindent()
            add(")")
        }

        Dynamic,
        is LambdaTypeName,
        is TypeVariableName,
        is WildcardTypeName -> error("Incorrect Partial interface type")
    }.let {}
}


fun HA_Type.partialToPartialInterface(model: HttpApiEntitiesById): TypeName {
    fun TypeName?.orNothing(): TypeName = this ?: NOTHING

    fun HA_Type.recurse() = partial().partial?.partialToPartialInterface(model).orNothing()

    return when (this) {
        is HA_Type.Primitive, is HA_Type.Enum, is HA_Type.Array, is HA_Type.Map -> error("Such partials should not be returned")
        is HA_Type.Object -> when (kind) {
            PAIR -> apiPairPartialType.parameterizedBy(
                firstType().recurse(),
                secondType().recurse()
            )
            TRIPLE -> apiTriplePartialType.parameterizedBy(
                firstType().recurse(),
                secondType().recurse(),
                thirdType().recurse()
            )
            MOD -> modPartialType.parameterizedBy(modSubjectType().recurse())
            BATCH, REQUEST_BODY -> error("Such partials should not be returned")
        }
        is HA_Type.Dto -> model.resolveDto(this).getClassName().dtoToPartialInterface()
        is HA_Type.Ref -> model.resolveDto(this).getClassName().dtoToPartialInterface()
        is HA_Type.UrlParam -> model.resolveUrlParam(this).getClassName().dtoToPartialInterface()
    }
}

private data class ChildFields(val ownFieldNamesToPartials: Map<String, HA_Type?>, val childFieldNamesToPartials: Map<String, List<HA_Type?>>) {
    val allFieldNamesToPartials: Map<String, List<HA_Type?>> = childFieldNamesToPartials.toMutableMap().also { all ->
        ownFieldNamesToPartials.forEach { all[it.key] = all.getOrDefault(it.key, emptyList()) + it.value }
    }
}

private fun getChildFields(model: HttpApiEntitiesById): Map<TID, ChildFields> {
    val result = mutableMapOf<TID, ChildFields>()
    fun addToResult(dto: HA_Dto): ChildFields {
        val childFields = mutableMapOf<String, MutableList<HA_Type?>>()
        dto.inheritors.forEach { inheritorRef ->
            addToResult(model.resolveDto(inheritorRef)).allFieldNamesToPartials.forEach { (fieldName, partials) ->
                partials.forEach {
                    childFields.getOrPut(fieldName) { mutableListOf() }.add(it)
                }
            }
        }
        return ChildFields(childFieldNamesToPartials = childFields, ownFieldNamesToPartials = dto.fields.associate {
            val partialDetectionResult = it.field.type.partial()
            require(partialDetectionResult.special == null) { "Batch fields are currently not supported" }
            it.field.name to partialDetectionResult.partial
        }).also { result[dto.id] = it }
    }
    model.dtoAndUrlParams.values.forEach {
        if (it.extends == null) addToResult(it)
    }
    return result
}

private fun fieldsRequiringJvmName(model: HttpApiEntitiesById, cfById: Map<TID, ChildFields>): Map<TID, Set<String>> {
    val result = mutableMapOf<TID, Set<String>>()
    fun impl(dto: HA_Dto, current: Set<String>) {
        val res = current + cfById.getValue(dto.id).allFieldNamesToPartials
            .mapNotNullTo(mutableSetOf<String>()) { (name, types) ->
                if (types.mapNotNullTo(mutableSetOf()) { it }.size > 1) name else null
            }
        dto.inheritors.forEach {
            impl(model.resolveDto(it), res)
        }
        result[dto.id] = res
    }
    model.dtoAndUrlParams.values.forEach {
        if (it.extends == null) impl(it, emptySet())
    }
    return result
}
