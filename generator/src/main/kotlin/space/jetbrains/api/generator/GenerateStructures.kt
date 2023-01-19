package space.jetbrains.api.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

private fun CodeBlock.Builder.appendPropertyDelegate(field: HA_DtoField, model: HttpApiEntitiesById) =
    appendPropertyDelegate(field.field.nullableTypeIfRequired(), model, field.requiresOption, field.extension)

private fun CodeBlock.Builder.appendPropertyDelegate(
    type: HA_Type,
    model: HttpApiEntitiesById,
    option: Boolean,
    isExtension: Boolean
): CodeBlock.Builder {
    val isExtensionArg = if (isExtension) "isExtension = true" else ""
    when (type) {
        is HA_Type.Primitive -> when (type.primitive) {
            HA_Primitive.Byte -> add("byte($isExtensionArg)")
            HA_Primitive.Short -> add("short($isExtensionArg)")
            HA_Primitive.Int -> add("int($isExtensionArg)")
            HA_Primitive.Long -> add("long($isExtensionArg)")
            HA_Primitive.Float -> add("float($isExtensionArg)")
            HA_Primitive.Double -> add("double($isExtensionArg)")
            HA_Primitive.Boolean -> add("boolean($isExtensionArg)")
            HA_Primitive.String -> if (PERMISSION_SCOPE_TAG in type.tags) {
                add("obj(%T", permissionScopeStructureType)
                if (isExtension) add(", $isExtensionArg")
                add(")")
            } else {
                add("string($isExtensionArg)")
            }
            HA_Primitive.Date -> add("date($isExtensionArg)")
            HA_Primitive.DateTime -> add("datetime($isExtensionArg)")
            HA_Primitive.Duration -> add("duration($isExtensionArg)")
        }
        is HA_Type.Array -> {
            add("list(")
            appendPropertyDelegate(type.elementType, model, false, isExtension)
            add(")")
        }
        is HA_Type.Map -> {
            add("map(")
            appendPropertyDelegate(type.valueType, model, false, isExtension)
            add(")")
        }
        is HA_Type.Object, is HA_Type.Dto, is HA_Type.Ref, is HA_Type.UrlParam -> {
            add("obj(")
            appendStructure(type, model)
            if (isExtension) add(", $isExtensionArg")
            add(")")
        }
        is HA_Type.Enum -> {
            add("enum<%T>($isExtensionArg)", type.copy(nullable = false).kotlinPoet(model))
        }
    }.let {}

    if (type.nullable) add(".nullable()")
    if (option) add(".optional()")

    return this
}

fun generateStructures(model: HttpApiEntitiesById): List<FileSpec> {
    val fieldDescriptorsByDtoId = model.buildFieldsByDtoId()

    return model.dtoAndUrlParams.values.mapNotNull { root ->
        if (root.extends != null) return@mapNotNull null

        val rootClassName = root.getClassName()
        if (rootClassName == batchInfoType) return@mapNotNull null

        val rootStructureClassName = rootClassName.getStructureClassName()

        FileSpec.builder(rootStructureClassName.packageName, rootStructureClassName.simpleName).apply {
            indent(INDENT)

            addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "ClassName")
                    .addMember("%S", "UnusedImport")
                    .addMember("%S", "REDUNDANT_ELSE_IN_WHEN")
                    .addMember("%S", "RemoveExplicitTypeArguments")
                    .addMember("%S", "KotlinRedundantDiagnosticSuppress")
                    .build()
            )

            root.subclasses(model).forEach { dto ->
                val dtoClassName = dto.getClassName()
                val dtoStructureClassName = dtoClassName.getStructureClassName()
                Log.info {
                    "Generating structure for '${dto.name}'"
                }
                addType(TypeSpec.objectBuilder(dtoStructureClassName).also { typeBuilder ->

                    typeBuilder.superclass(typeStructureType.parameterizedBy(dtoClassName))
                    typeBuilder.addSuperclassConstructorParameter(dto.record.toString())

                    val fields = fieldDescriptorsByDtoId.getValue(dto.id)

                    typeBuilder.addProperties(fields.map {
                        PropertySpec.builder(
                            name = it.field.name,
                            type = propertyType.importNested().parameterizedBy(it.field.fieldKotlinPoetType(model)),
                            modifiers = listOf(KModifier.PRIVATE),
                        ).delegate(buildCodeBlock { appendPropertyDelegate(it.field, model) })
                            .build()
                    })

                    typeBuilder.addFunction(FunSpec.builder("deserialize").also func@{ funcBuilder ->
                        funcBuilder.addModifiers(KModifier.OVERRIDE)
                        funcBuilder.addParameter("context", deserializationContextType)
                        funcBuilder.returns(dtoClassName)

                        if (dto.id in model.urlParams) {
                            funcBuilder.beginControlFlow(
                                "context.json?.%M()?.let·{ compactId ->",
                                asStringOrNullFunction
                            )
                            val urlParam = model.urlParams.getValue(dto.id)
                            funcBuilder.addCode("val (fieldNames, json) = compactIdToFieldNamesAndJson(compactId)\n")
                            funcBuilder.addCode("val newContext = context.copy(json = json)\n")
                            funcBuilder.beginControlFlow("return·when (fieldNames) {")
                            urlParam.options.forEach { option ->
                                when (option) {
                                    is HA_UrlParameterOption.Const -> {
                                        funcBuilder.addCode("setOf(%S", option.value)
                                    }
                                    is HA_UrlParameterOption.Var -> {
                                        funcBuilder.addCode("setOf(")
                                        option.parameters.forEachIndexed { i, param ->
                                            if (i != 0) funcBuilder.addCode(", ")
                                            funcBuilder.addCode("%S", param.name)
                                        }
                                    }
                                }
                                funcBuilder.addCode(
                                    ") -> %T.deserialize(newContext)\n",
                                    option.getClassName().getStructureClassName()
                                )
                            }
                            funcBuilder.addCode("else -> ")
                            urlParam.options.firstNotNullOfOrNull { it as? HA_UrlParameterOption.Var }?.let {
                                funcBuilder.addCode(
                                    "%T.deserialize(newContext)\n",
                                    it.getClassName().getStructureClassName()
                                )
                            } ?: funcBuilder.addCode(
                                "minorDeserializationError(\"Unsupported parameter set: '\$fieldNames'\", context.link)\n"
                            )

                            funcBuilder.endControlFlow()
                            funcBuilder.endControlFlow()
                        }

                        val codeReferences = mutableListOf<Any>()

                        val createInstance = buildString {
                            when {
                                dto.isObject -> append("%T")
                                fields.isNotEmpty() -> {
                                    append("%T(\n$INDENT")
                                    fields.forEachIndexed { i, field ->
                                        if (i != 0) append(",\n$INDENT")
                                        append("${field.field.name} = this.${field.field.name}.deserialize(context)")
                                    }
                                    append("\n)")
                                }
                                else -> append("%T()")
                            }
                        }

                        val toReturn = if (dto.inheritors.isEmpty() && !dto.hierarchyRole2.isAbstract) {
                            codeReferences += dtoClassName
                            createInstance
                        } else {
                            "when (val className = context.className()) {" +
                                dto.inheritors.joinToString("\n$INDENT", "\n$INDENT", "\n") {
                                    val inheritor = model.resolveDto(it)
                                    val inheritorClassName = inheritor.getClassName()
                                    codeReferences += inheritorClassName.getStructureClassName()
                                    val condition = "\"${inheritor.name}\"" + if (inheritor.inheritors.isNotEmpty()) {
                                        codeReferences += inheritorClassName.getStructureClassName()
                                        ", in %T.childClassNames"
                                    } else ""
                                    val withActualTypeCall = if (inheritor.inheritors.isEmpty()) ".withActualType(className)" else ""
                                    "$condition -> %T.deserialize(context$withActualTypeCall)"
                                } +
                                (if (!dto.hierarchyRole2.isAbstract) {
                                    codeReferences += dtoClassName
                                    "$INDENT\"${dto.name}\" -> " +
                                        createInstance.indentNonFirst() + "\n"
                                } else "") +
                                "${INDENT}else -> minorDeserializationError(\"Unsupported class name: '\$className'\", context.link)\n}"
                        }
                        if (dto.id in model.urlParams) {
                            funcBuilder.addCode("return·$toReturn", *codeReferences.toTypedArray())
                        } else {
                            funcBuilder.addCode("return $toReturn", *codeReferences.toTypedArray())
                        }
                    }.build())

                    typeBuilder.addFunction(FunSpec.builder("serialize").also func@{ func ->
                        func.addModifiers(KModifier.OVERRIDE)
                        func.addParameter("value", dtoClassName)
                        func.returns(jsonValueType)

                        if (dto.id in model.urlParams || dto.extends?.id in model.urlParams) {
                            func.addCode("return %M(value.compactId)", jsonStringFunction)
                            return@func
                        }

                        val codeReferences = mutableListOf<Any>()

                        val createJson = "%M(listOfNotNull(" + (fields.takeIf { it.isNotEmpty() }
                            ?.joinToString(",\n$INDENT", "\n$INDENT", "\n") {
                                if (it.field.field.requiresAddedNullability) {
                                    "value.${it.field.name}?.let·{ this.${it.field.name}.serialize(it) }"
                                } else {
                                    "this.${it.field.name}.serialize(value.${it.field.name})"
                                }
                            } ?: "") + "))"

                        val toReturn = if (dto.inheritors.isEmpty() && !dto.hierarchyRole2.isAbstract) {
                            codeReferences += jsonObjectFunction
                            createJson
                        } else {
                            "when (value) {" +
                                dto.inheritors.joinToString("\n$INDENT", "\n$INDENT", "\n") {
                                    val inheritor = model.resolveDto(it)
                                    val inheritorClassName = inheritor.getClassName()
                                    codeReferences += inheritorClassName
                                    codeReferences += inheritorClassName.getStructureClassName()
                                    "is %T -> %T.serialize(value).withClassName(\"${inheritor.name}\")"
                                } +
                                "${INDENT}else -> " +
                                if (!dto.hierarchyRole2.isAbstract) {
                                    codeReferences += jsonObjectFunction
                                    createJson.indentNonFirst() + ".withClassName(\"${dto.name}\")"
                                } else {
                                    "error(\"Unsupported class: '\${value::class.simpleName}'\")"
                                } +
                                "\n}"
                        }

                        func.addCode("return $toReturn", *codeReferences.toTypedArray())
                    }.build())

                    if (dto.inheritors.isNotEmpty()) {
                        typeBuilder.addProperty(
                            PropertySpec.builder("childClassNames", SET.parameterizedBy(STRING), KModifier.OVERRIDE)
                                .initializer(buildCodeBlock {
                                    add("setOf(")
                                    dto.inheritors.forEachIndexed { i, it ->
                                        if (i != 0) add(", ")
                                        add("%S", model.resolveDto(it).name)
                                    }
                                    add(")")
                                    dto.inheritors.forEach {
                                        val inheritor = model.resolveDto(it)
                                        if (inheritor.inheritors.isNotEmpty()) {
                                            add("·+ %T.childClassNames", inheritor.getClassName().getStructureClassName())
                                        }
                                    }
                                })
                                .build()
                        )
                    }
                }.build())
            }
        }.build()
    }
}

fun HA_DtoField.fieldKotlinPoetType(model: HttpApiEntitiesById): TypeName = if (
    field.type is HA_Type.Primitive && field.type.primitive == HA_Primitive.String &&
    PERMISSION_SCOPE_TAG in field.type.tags
) {
    permissionScopeType.copy(nullable = field.nullableTypeIfRequired().nullable, option = requiresOption)
} else {
    field.nullableTypeIfRequired().kotlinPoet(model, requiresOption)
}

private fun String.indentNonFirst() = if ('\n' in this) {
    substringBefore('\n') + '\n' + substringAfter('\n').prependIndent(INDENT)
} else this
