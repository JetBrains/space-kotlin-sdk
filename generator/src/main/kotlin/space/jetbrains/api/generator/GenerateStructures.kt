package space.jetbrains.api.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

private fun StringBuilder.appendPropertyDelegate(type: HA_Type, types: MutableList<TypeName>, model: HttpApiEntitiesById): StringBuilder {
    when (type) {
        is HA_Type.Primitive -> when (type.primitive) {
            HA_Primitive.Byte -> append("byte()")
            HA_Primitive.Short -> append("short()")
            HA_Primitive.Int -> append("int()")
            HA_Primitive.Long -> append("long()")
            HA_Primitive.Float -> append("float()")
            HA_Primitive.Double -> append("double()")
            HA_Primitive.Boolean -> append("boolean()")
            HA_Primitive.String -> append("string()")
            HA_Primitive.Date -> append("date()")
            HA_Primitive.DateTime -> append("datetime()")
        }

        is HA_Type.Array -> {
            val elementType = type.elementType
            if (elementType is HA_Type.Object && elementType.kind == HA_Type.Object.Kind.MAP_ENTRY) {
                append("map(")
                appendPropertyDelegate(elementType.keyType(), types, model)
                append(", ")
                appendPropertyDelegate(elementType.valueType(), types, model)
            } else {
                append("list(")
                appendPropertyDelegate(elementType, types, model)
            }
            append(')')
        }
        is HA_Type.Object, is HA_Type.Dto, is HA_Type.Ref -> {
            append("obj(")
            appendStructure(type, types, model)
            append(')')
        }
        is HA_Type.Enum -> {
            types += type.copy(nullable = false, optional = false).kotlinPoet(model)
            append("enum<%T>()")
        }
        is HA_Type.UrlParam -> {
            append("urlParam()") // TODO: Support UrlParam
        }
    }.let {}

    if (type.nullable) append(".nullable()")
    if (type.optional) append(".optional()")

    return this
}

fun generateStructures(model: HttpApiEntitiesById): List<FileSpec> {
    val fieldDescriptorsByDtoId = model.buildFieldsByDtoId()

    return model.dto.values.mapNotNull { root ->
        if (root.extends != null) return@mapNotNull null

        val rootClassName = root.getClassName()
        if (rootClassName == batchInfoType) return@mapNotNull null

        val rootStructureClassName = rootClassName.getStructureClassName()

        FileSpec.builder(rootStructureClassName.packageName, rootStructureClassName.simpleName).apply {
            indent(INDENT)

            necessaryImports.forEach {
                addImport(it.packageName, it.simpleNames.joinToString("."))
            }

            root.subclasses(model).forEach { dto ->
                val dtoClassName = dto.getClassName()
                val dtoStructureClassName = dtoClassName.getStructureClassName()

                addType(TypeSpec.objectBuilder(dtoStructureClassName).also { typeBuilder ->

                    typeBuilder.superclass(typeStructureType.parameterizedBy(dtoClassName))

                    val fields = fieldDescriptorsByDtoId.getValue(dto.id)

                    typeBuilder.addProperties(fields.map {
                        val delegateTypes = mutableListOf<TypeName>()

                        val pseudoPackageName = propertyType.packageName + "." + propertyType.simpleNames.dropLast(1).joinToString(".")
                        val propertyTypePseudo = ClassName(pseudoPackageName, propertyType.simpleName)

                        PropertySpec.builder(it.field.name, propertyTypePseudo.parameterizedBy(it.field.type.kotlinPoet(model)))
                            .delegate(
                                buildString { appendPropertyDelegate(it.field.type, delegateTypes, model) },
                                *delegateTypes.toTypedArray()
                            )
                            .build()
                    })

                    typeBuilder.addFunction(FunSpec.builder("deserialize").also { funcBuilder ->
                        funcBuilder.addModifiers(KModifier.OVERRIDE)
                        funcBuilder.addParameter("context", deserializationContextType.parameterizedBy(
                            WildcardTypeName.consumerOf(dtoClassName)
                        ))
                        funcBuilder.returns(dtoClassName)

                        val codeReferences = mutableListOf<Any>()

                        val createInstance = "%T(" + (fields.takeIf { it.isNotEmpty() }
                            ?.joinToString(",\n$INDENT", "\n$INDENT", "\n") {
                                "${it.field.name} = this.${it.field.name}.deserialize(context)"
                            } ?: "") + ")"

                        val toReturn = if (dto.inheritors.isEmpty() && !dto.hierarchyRole.isAbstract) {
                            codeReferences += dtoClassName
                            createInstance
                        } else {
                            "when (val className = ${stringTypeType.simpleName}.deserialize(context.child(\"className\"))) {" +
                                dto.inheritors.joinToString("\n$INDENT", "\n$INDENT", "\n") {
                                    val inheritor = model.resolveDto(it)
                                    val inheritorClassName = inheritor.getClassName()
                                    codeReferences += inheritorClassName.getStructureClassName()
                                    "\"${inheritor.name}\" -> %T.deserialize(context)"
                                } +
                                (if (!dto.hierarchyRole.isAbstract) {
                                    codeReferences += dtoClassName
                                    "$INDENT\"${dto.name}\" -> " +
                                        createInstance.indentNonFirst() + "\n"
                                } else "") +
                                "${INDENT}else -> error(\"Unsupported class name: '\$className'\")\n}"
                        }

                        funcBuilder.addCode("return·$toReturn", *codeReferences.toTypedArray())
                    }.build())

                    typeBuilder.addFunction(FunSpec.builder("serialize").apply {
                        addModifiers(KModifier.OVERRIDE)
                        addParameter("value", dtoClassName)
                        returns(jsonValueType)

                        val codeReferences = mutableListOf<Any>()

                        val createJson = "%M(listOfNotNull(" + (fields.takeIf { it.isNotEmpty() }
                            ?.joinToString(",\n$INDENT", "\n$INDENT", "\n") {
                                "this.${it.field.name}.serialize(value.${it.field.name})"
                            } ?: "") + "))"

                        val toReturn = if (dto.inheritors.isEmpty() && !dto.hierarchyRole.isAbstract) {
                            codeReferences += jsonObjectFunction
                            createJson
                        } else {
                            "when (value) {" +
                                dto.inheritors.joinToString("\n$INDENT", "\n$INDENT", "\n") {
                                    val inheritor = model.resolveDto(it)
                                    val inheritorClassName = inheritor.getClassName()
                                    codeReferences += inheritorClassName
                                    codeReferences += inheritorClassName.getStructureClassName()
                                    "is %T -> %T.serialize(value).withClassName(\"${inheritorClassName.simpleName}\")"
                                } +
                                "${INDENT}else -> " +
                                if (!dto.hierarchyRole.isAbstract) {
                                    codeReferences += jsonObjectFunction
                                    createJson.indentNonFirst() + ".withClassName(\"${dtoClassName.simpleName}\")"
                                } else {
                                    "error(\"Unsupported class\")"
                                } +
                                "\n}"
                        }

                        addCode("return·$toReturn", *codeReferences.toTypedArray())
                    }.build())

                    typeBuilder.addProperty(
                        PropertySpec.builder(
                            "defaultPartialFull", LambdaTypeName.get(
                                partialType.parameterizedBy(WildcardTypeName.consumerOf(dtoClassName)),
                                returnType = UNIT
                            )
                        ).also { propBuilder ->
                            propBuilder.addModifiers(KModifier.OVERRIDE)

                            val codeReferences = mutableListOf<ClassName>()
                            val inner = fields
                                .filter { !it.isExtension }
                                .joinToString("\n$INDENT", prefix = INDENT, postfix = "\n") {
                                    it.field.name + "()"
                                } + if (dto.inheritors.isNotEmpty()) {
                                dto.inheritors.joinToString("\n$INDENT", prefix = INDENT, postfix = "\n") {
                                    codeReferences += model.resolveDto(it).getClassName().getStructureClassName()
                                    "%T.defaultPartialFull(this)"
                                }
                            } else ""
                            propBuilder.initializer("{\n$inner}", *codeReferences.toTypedArray())
                        }.build()
                    )


                    if (dto.record) {
                        typeBuilder.addProperty(
                            PropertySpec.builder(
                                "defaultPartialCompact", LambdaTypeName.get(
                                    partialType.parameterizedBy(WildcardTypeName.consumerOf(dtoClassName)),
                                    returnType = UNIT
                                )
                            ).also { propBuilder ->
                                propBuilder.addModifiers(KModifier.OVERRIDE)

                                if (dto.inheritors.isNotEmpty()) {
                                    val codeReferences = mutableListOf<ClassName>()
                                    propBuilder.initializer(
                                        dto.inheritors.joinToString("\n$INDENT", prefix = "{\n$INDENT", postfix = "\n}") {
                                            codeReferences += model.resolveDto(it).getClassName().getStructureClassName()
                                            "%T.defaultPartialCompact(this)"
                                        }, *codeReferences.toTypedArray()
                                    )
                                } else propBuilder.initializer("{ id() }")

                            }.build()
                        )
                    }

                }.build())

                dto.fields.forEach {
                    val type = it.field.type
                    val (partial, specialPartial, isRef) = type.partial()
                    val funcBuilder = FunSpec.builder(it.field.name)
                        .receiver(partialType.parameterizedBy(WildcardTypeName.consumerOf(dtoClassName)))
                        .addAnnotation(
                            AnnotationSpec.builder(JvmName::class)
                                .addMember("\"partial-${dtoClassName.simpleName}-${it.field.name}\"")
                                .build()
                        )

                    if (partial == null) {
                        funcBuilder.addStatement("add(%T.${it.field.name})", dtoStructureClassName)
                    } else {
                        addFunction(
                            FunSpec.builder(it.field.name)
                                .receiver(partialType.parameterizedBy(WildcardTypeName.consumerOf(dtoClassName)))
                                .addAnnotation(
                                    AnnotationSpec.builder(JvmName::class)
                                        .addMember("\"partial-${dtoClassName.simpleName}-${it.field.name}-recursively\"")
                                        .build()
                                )
                                .addParameter(
                                    "recursiveAs",
                                    partialType.parameterizedBy(WildcardTypeName.consumerOf(partial.kotlinPoet(model)))
                                )
                                .addStatement("addRecursively(%T.${it.field.name}, recursiveAs)", dtoStructureClassName)
                                .build()
                        )

                        val (partialStructure, partialStructureTypes) = structureCode(partial, model)
                        funcBuilder.addParameter(
                            ParameterSpec.builder(
                                "build",
                                LambdaTypeName.get(
                                    receiver = partialType.parameterizedBy(partial.kotlinPoet(model)),
                                    returnType = UNIT
                                )
                            ).defaultValue(
                                format = "$partialStructure.defaultPartial" + if (isRef) "Compact" else "Full",
                                args = *partialStructureTypes
                            ).build()
                        )

                        val statement = "add(%T.${it.field.name}, $partialStructure, build" +
                            if (specialPartial != null) {
                                addImport(partialSpecialType, specialPartial.name)
                                ", $specialPartial)"
                            } else ")"

                        funcBuilder.addStatement(statement, dtoStructureClassName, *partialStructureTypes)
                    }

                    addFunction(funcBuilder.build())
                }
            }
        }.build()
    }
}

private fun String.indentNonFirst() = if ('\n' in this) {
    substringBefore('\n') + '\n' + substringAfter('\n').prependIndent(INDENT)
} else this
