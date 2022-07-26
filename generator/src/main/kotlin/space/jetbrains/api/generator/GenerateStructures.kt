package space.jetbrains.api.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

private fun CodeBlock.Builder.appendPropertyDelegate(field: HA_DtoField, model: HttpApiEntitiesById) =
    appendPropertyDelegate(field.type, model, field.requiresOption, field.extension)

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
            HA_Primitive.String -> add("string($isExtensionArg)")
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
                            type = propertyType.importNested().parameterizedBy(it.field.type.kotlinPoet(model)),
                            modifiers = listOf(KModifier.PRIVATE),
                        ).delegate(buildCodeBlock { appendPropertyDelegate(it.field, model) })
                            .build()
                    })

                    typeBuilder.addFunction(FunSpec.builder("deserialize").also { funcBuilder ->
                        funcBuilder.addModifiers(KModifier.OVERRIDE)
                        funcBuilder.addParameter("context", deserializationContextType)
                        funcBuilder.returns(dtoClassName)

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
                            codeReferences += stringTypeType.importNested()
                            "when (val className = %T.deserialize(context.child(\"className\"))) {" +
                                dto.inheritors.joinToString("\n$INDENT", "\n$INDENT", "\n") {
                                    val inheritor = model.resolveDto(it)
                                    val inheritorClassName = inheritor.getClassName()
                                    codeReferences += inheritorClassName.getStructureClassName()
                                    val condition = "\"${inheritor.name}\"" + if (inheritor.inheritors.isNotEmpty()) {
                                        codeReferences += inheritorClassName.getStructureClassName()
                                        ", in %T.childClassNames"
                                    } else ""
                                    "$condition -> %T.deserialize(context)"
                                } +
                                (if (!dto.hierarchyRole2.isAbstract) {
                                    codeReferences += dtoClassName
                                    "$INDENT\"${dto.name}\" -> " +
                                        createInstance.indentNonFirst() + "\n"
                                } else "") +
                                "${INDENT}else -> minorDeserializationError(\"Unsupported class name: '\$className'\", context.link)\n}"
                        }

                        funcBuilder.addCode("return $toReturn", *codeReferences.toTypedArray())
                    }.build())

                    typeBuilder.addFunction(FunSpec.builder("serialize").also { func ->
                        func.addModifiers(KModifier.OVERRIDE)
                        func.addParameter("value", dtoClassName)
                        func.returns(jsonValueType)

                        val codeReferences = mutableListOf<Any>()

                        val createJson = "%M(listOfNotNull(" + (fields.takeIf { it.isNotEmpty() }
                            ?.joinToString(",\n$INDENT", "\n$INDENT", "\n") {
                                "this.${it.field.name}.serialize(value.${it.field.name})"
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

private fun String.indentNonFirst() = if ('\n' in this) {
    substringBefore('\n') + '\n' + substringAfter('\n').prependIndent(INDENT)
} else this
