package space.jetbrains.api.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import space.jetbrains.api.generator.HA_Type.Object.Kind.*

fun generatePartials(model: HttpApiEntitiesById): List<FileSpec> {
    val childFieldsById = getChildFields(model)
    val fieldsRequiringJvmName = fieldsRequiringJvmName(model, childFieldsById)

    val notInputOnlyDtos = model.dtoAndUrlParams.keys - findInputOnlyDtoIds(model)
    return notInputOnlyDtos.map { model.dtoAndUrlParams.getValue(it) }.mapNotNull { root ->
        if (root.extends != null && root.extends.id in notInputOnlyDtos) return@mapNotNull null

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

            file.addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "OptIn")).also { ann ->
                    model.featureFlags.values.forEach {
                        ann.addMember("%T::class", it.annotationClassName())
                    }
                }.build()
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

                fun FunSpec.Builder.addJvmName(fieldName: String, partialInterface: TypeName, recursive: Boolean = false) {
                    fun getShortId(typeName: TypeName): String = when (typeName) {
                        is ClassName -> typeName.simpleNames.joinToString("")
                        is ParameterizedTypeName -> getShortId(typeName.rawType) + "_" +
                            typeName.typeArguments.joinToString("_") { getShortId(it) }
                        is WildcardTypeName, is TypeVariableName, is LambdaTypeName, Dynamic -> error("incorrect type")
                    }

                    addAnnotation(
                        AnnotationSpec.builder(JvmName::class)
                            .addMember("%S", fieldName + "_" + getShortId(partialInterface) + if (recursive) "-r" else "")
                            .build()
                    )
                }

                fun addImplBodyWithPartial(
                    impl: FunSpec.Builder,
                    fieldName: String,
                    buildPartialParameter: ParameterSpec,
                    partialInterface: TypeName,
                    batch: Boolean,
                ) {
                    if (fieldName in requiringJvmName) {
                        impl.addJvmName(fieldName, partialInterface)
                    }

                    impl.addParameter(buildPartialParameter)

                    impl.addCode(buildCodeBlock {
                        add("builder.add(%S, ", fieldName)
                        partialImplConstructorLambda(partialInterface)
                        add(", build")
                        if (batch) {
                            add(", isBatch = true")
                        }
                        add(")")
                    })
                }

                fun addRecursiveAsImpl(fieldName: String, partialInterface: TypeName) {
                    implBuilder.addFunction(
                        FunSpec.builder(fieldName)
                            .addModifiers(PUBLIC, OVERRIDE)
                            .addParameter("recursiveAs", partialInterface)
                            .addStatement("builder.addRecursively(%S, getPartialBuilder(recursiveAs))", fieldName)
                            .also { if (fieldName in requiringJvmName) it.addJvmName(fieldName, partialInterface, true) }
                            .build()
                    )
                }

                dto.fields.forEach { dtoField ->
                    val field = dtoField.field
                    val (partial, batch) = field.type.partial()

                    val interfaceFunBuilder = FunSpec.builder(field.name).addModifiers(PUBLIC, ABSTRACT)
                    val implFunBuilder = FunSpec.builder(field.name).addModifiers(PUBLIC, OVERRIDE)

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
                                .addModifiers(PUBLIC, ABSTRACT)
                                .addParameter("recursiveAs", partialInterface)
                                .also { if (isOverride) it.addModifiers(PUBLIC, OVERRIDE) }
                                .also { if (field.name in requiringJvmName) it.addJvmName(field.name, partialInterface, true) }
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
                        addImplBodyWithPartial(implFunBuilder, field.name, buildPartialParameter, partialInterface, batch)
                    }

                    interfaceBuilder.addFunction(interfaceFunBuilder.build())
                    implBuilder.addFunction(implFunBuilder.build())
                }

                fun addImpl(fieldName: String, partialInterface: TypeName?, batch: Boolean) {
                    val implFunBuilder = FunSpec.builder(fieldName).addModifiers(PUBLIC, OVERRIDE)

                    if (partialInterface == null) {
                        implFunBuilder.addStatement("builder.add(%S)", fieldName)
                    } else {
                        val buildPartialParameter = ParameterSpec.builder(
                            name = "build",
                            type = LambdaTypeName.get(receiver = partialInterface, returnType = UNIT)
                        ).build()
                        addImplBodyWithPartial(implFunBuilder, fieldName, buildPartialParameter, partialInterface, batch)
                    }

                    implBuilder.addFunction(implFunBuilder.build())
                }

                cf.childFieldNamesToPartials.asSequence()
                    .filter { it.key !in cf.ownFieldNamesToPartials }
                    .flatMap { (fieldName, partials) ->
                        partials.groupingBy { it?.partialToPartialInterface(model) }
                            .eachCount()
                            .asSequence()
                            .filter { it.value > 1 }
                            .map { fieldName to it.key }
                    }.forEach { (fieldName, partialInterface) ->
                        addImpl(fieldName, partialInterface, false)
                        partialInterface?.let { addRecursiveAsImpl(fieldName, it) }
                    }

                if (dto.inheritors.isNotEmpty()) {
                    implBuilder.addFunction(
                        FunSpec.builder("defaultPartial")
                            .addModifiers(PUBLIC, OVERRIDE)
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

private fun findInputOnlyDtoIds(model: HttpApiEntitiesById): Set<String> {
    val visitedIds = mutableSetOf<String>()
    fun traverseType(type: HA_Type) {
        fun traverseDto(dto: HA_Dto) {
            if (!visitedIds.add(dto.id)) return
            dto.fields.forEach {
                traverseType(it.type)
            }
            dto.inheritors.forEach {
                traverseDto(model.resolveDto(it))
            }
        }
        when (type) {
            is HA_Type.Array -> traverseType(type.elementType)
            is HA_Type.Dto -> traverseDto(model.resolveDto(type))
            is HA_Type.Map -> traverseType(type.valueType)
            is HA_Type.Object -> type.fields.forEach { traverseType(it.type) }
            is HA_Type.Enum, is HA_Type.Primitive -> {}
            is HA_Type.Ref -> traverseDto(model.resolveDto(type))
            is HA_Type.UrlParam -> traverseDto(model.resolveUrlParam(type))
        }
    }

    model.resources.values.asSequence()
        .flatMap { it.endpoints.asSequence() }
        .forEach { endpoint -> endpoint.responseBody?.let { traverseType(it) } }

    // at this point visitedIds contains DTOs used in outputs
    val outputDtos = visitedIds.toSet()

    model.resources.values.asSequence()
        .flatMap { it.endpoints.asSequence() }
        .forEach { endpoint ->
            endpoint.parameters.forEach { traverseType(it.field.type) }
            (endpoint.requestBody as? HA_Type.Object)?.let { traverseType(it) }
        }

    // at this point visitedIds contains both DTOs used in inputs and in outputs
    // notInputOrOutputDtos contains DTOs not used in endpoints at all, most notably - ApplicationPayload
    // these exceptions will need to have generated partials, as those can be customized for webhook payload
    val notInputOrOutputDtos = model.dtoAndUrlParams.keys - visitedIds
    visitedIds.clear()

    notInputOrOutputDtos.forEach {
        traverseType(HA_Type.Dto(HA_Dto.Ref(it), false, emptyList()))
    }

    // at this point visitedIds contains notInputOrOutputDtos and everything that these DTOs depend on
    // they don't need to be considered "input-only" for the reason above
    return model.dtoAndUrlParams.keys - outputDtos - visitedIds
}

private fun CodeBlock.Builder.partialImplConstructorLambda(partialInterfaceOrNothing: TypeName, newlines: Boolean = true) {
    if (partialInterfaceOrNothing == NOTHING) {
        add("%T.throwPrimitivesAndEnumsError", partialImplType)
        return
    }
    add("{")
    if (newlines) {
        add("\n")
        indent()
    } else {
        add(" ")
    }
    partialImplConstructor(partialInterfaceOrNothing, "it")
    if (newlines) {
        add("\n")
        unindent()
    } else {
        add(" ")
    }
    add("}")
}

fun CodeBlock.Builder.partialImplConstructor(partialInterfaceOrNothing: TypeName, partialBuilderVar: String) {
    return when (partialInterfaceOrNothing) {
        NOTHING -> {
            add("%T.throwPrimitivesAndEnumsError()", partialImplType)
        }
        is ClassName -> {
            add("%T($partialBuilderVar)", partialInterfaceOrNothing.partialInterfaceToImpl())
        }

        is ParameterizedTypeName -> {
            val impl = partialInterfaceOrNothing.rawType.partialInterfaceToImpl()
            add("%T(\n", impl)
            indent()
            partialInterfaceOrNothing.typeArguments.forEach {
                partialImplConstructorLambda(it, newlines = false)
                add(",\n")
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
    fun HA_Type.recurse() = partial().partial?.partialToPartialInterface(model) ?: NOTHING

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
            BATCH, REQUEST_BODY, SYNC_BATCH -> error("Such partials should not be returned")
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
            require(!partialDetectionResult.batch) { "Batch fields are currently not supported" }
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
            .mapNotNullTo(mutableSetOf()) { (name, types) ->
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
