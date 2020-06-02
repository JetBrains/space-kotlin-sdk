package space.jetbrains.api.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.FunSpec.Builder
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import space.jetbrains.api.generator.HA_Method.*
import space.jetbrains.api.generator.HA_PathSegment.*

const val META_PARAMETERS_PREFIX = "$"

private fun resourcePackage(parentDisplayPath: Iterable<String>): String {
    return RESOURCES_PACKAGE + '.' + parentDisplayPath.joinToString(".") { it.displayNameToMemberName() }
}

fun generateResources(model: HttpApiEntitiesById): List<FileSpec> {
    return model.resources.values.groupBy { displayPath(it, model) }.map { (displayPath, resourceGroup) ->
        val className = ClassName(
            resourcePackage(displayPath.dropLast(1)),
            resourceGroup.first().displayPlural.displayNameToClassName()
        )
        FileSpec.builder(className.packageName, className.simpleName).also { fileBuilder ->
            fileBuilder.indent(INDENT)

            necessaryImports.forEach {
                fileBuilder.addImport(it.packageName, it.simpleNames.joinToString("."))
            }

            if (displayPath.size == 1) {
                fileBuilder.addProperty(
                    PropertySpec.builder(resourceGroup.first().displayPlural.displayNameToMemberName(), className)
                        .receiver(clientWithContextType)
                        .getter(FunSpec.getterBuilder().addStatement("return %T(this)", className).build())
                        .build()
                )
            }

            fileBuilder.addType(TypeSpec.classBuilder(className).also { typeBuilder ->
                typeBuilder.primaryConstructor(FunSpec.constructorBuilder().addParameter("client", clientWithContextType).build())
                typeBuilder.superclass(restResourceType)
                typeBuilder.addSuperclassConstructorParameter("client")

                typeBuilder.addProperties(
                    resourceGroup.asSequence()
                        .flatMap { it.nestedResources.asSequence() }
                        .groupBy { displayPath(it, model) }
                        .values
                        .map { nestedGroup ->
                            val nestedName = nestedGroup.first().displayPlural
                            val nestedType = ClassName(resourcePackage(displayPath), nestedName.displayNameToClassName())
                            PropertySpec
                                .builder(nestedName.displayNameToMemberName(), nestedType)
                                .initializer("%T(client)", nestedType)
                                .build()
                        }
                )

                typeBuilder.addFunctions(resourceGroup.flatMap { it.endpoints }.map { endpoint ->
                    val urlParams = endpoint.parameters.sortedBy { !it.path }.map { it.field }
                    val bodyParams = endpoint.requestBody?.fields
                    val returnType = endpoint.responseBody?.kotlinPoet(model)
                    val (partial, specialPartial) = endpoint.responseBody.partial()
                    val partialKP = partial?.kotlinPoet(model)
                    val partialStructure = partial?.let { structureCode(it, model) }
                    val hasUrlBatchInfo = urlParams.any { it.name == "\$skip" || it.name == "\$top" }

                    val (funcParams, deprecationKDoc) = getFuncParamsAndDeprecationKDoc(model, urlParams, bodyParams, hasUrlBatchInfo, partialStructure, partialKP)

                    val fullPath = endpoint.path.segments.asReversed().plus(
                        ancestors(model.resources.getValue(endpoint.resource.id), model)
                            .flatMap { it.path.segments.asReversed().asSequence() }
                    ).asReversed()

                    val funcName = endpoint.displayName.displayNameToMemberName()

                    FunSpec.builder(funcName).also { funcBuilder ->
                        val kDoc = when {
                            deprecationKDoc != null -> endpoint.doc?.let { "$it\n" }.orEmpty() + deprecationKDoc
                            else -> endpoint.doc
                        }

                        kDoc?.let { funcBuilder.addKdoc(it) }
                        funcBuilder.annotations.deprecation(endpoint.deprecation)
                        funcBuilder.addModifiers(KModifier.SUSPEND)
                        funcBuilder.addParameters(funcParams)
                        if (returnType != null) funcBuilder.returns(returnType)
                        if (partialKP != null && partialStructure != null) {
                            val specialArg = specialPartial?.let {
                                fileBuilder.addImport(partialSpecialType, it.name)
                                ", $it"
                            } ?: ""
                            funcBuilder.addStatement(
                                "val partial = %T(${partialStructure.first}$specialArg).apply(buildPartial)",
                                partialType, *partialStructure.second
                            )
                        }

                        val (httpCallFuncName, httpMethod) = httpCallFuncNameToMethod(endpoint)

                        endpoint.parameters.forEach {
                            parameterConversion(it, funcBuilder)
                        }

                        val path = fullPath.joinToString("/") {
                            when (it) {
                                is Const -> it.value
                                is Var -> "\${pathParam(" + it.name + ")}"
                                is PrefixedVar -> it.prefix + ":\${pathParam(" + it.name + ")}"
                            }
                        }
                        val batchInfoQueryArg = if (hasUrlBatchInfo) "\n${INDENT}appendBatchInfo(batchInfo)" else ""
                        val parametersBody = endpoint.parameters.asSequence()
                            .filter { !it.path && !it.field.name.startsWith(META_PARAMETERS_PREFIX) }
                            .toList()
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString("\n$INDENT", "\n$INDENT", "\n") {
                                val name = it.field.name
                                when (val type = it.field.type) {
                                    is HA_Type.Primitive,
                                    is HA_Type.Ref,
                                    is HA_Type.Enum -> {
                                        val toString = if (
                                            type is HA_Type.Primitive &&
                                            type.primitive == HA_Primitive.String) ""
                                        else ".toString()"

                                        if (it.field.type.nullable) {
                                            """$name?.let·{ append("$name", it$toString) }"""
                                        } else """append("$name", $name$toString)"""
                                    }

                                    is HA_Type.UrlParam -> {
                                        "" // TODO: Support UrlParam
                                    }

                                    is HA_Type.Array -> {
                                        val orEmpty = if (type.nullable) ".orEmpty()" else ""

                                        if (
                                            type.elementType is HA_Type.Primitive &&
                                            type.elementType.primitive == HA_Primitive.String) {
                                            """appendAll("$name", $name$orEmpty)"""
                                        } else {
                                            """appendAll("$name", $name$orEmpty.map·{ it.toString })"""
                                        }
                                    }


                                    is HA_Type.Object,
                                    is HA_Type.Dto -> error("Objects cannot occur in URL parameters")
                                }
                            } ?: ""

                        val references = mutableListOf<Any>(httpMethodType)
                        val parametersArg = (batchInfoQueryArg + parametersBody).takeIf {
                            it.isNotEmpty()
                        }?.let {
                            references.add(parametersType)
                            ", parameters = %T.build·{$it}"
                        } ?: ""

                        val bodyArg = endpoint.requestBody?.fields?.joinToString(",\n$INDENT", "\n$INDENT", "\n") {
                            val typeDesc = buildString { appendType(it.type, references, model) }
                            val name = it.name
                            if (it.type.optional) {
                                """$typeDesc.serialize($name)?.let·{ "$name"·to it }"""
                            } else {
                                """"$name"·to $typeDesc.serialize($name)"""
                            }
                        }?.let {
                            references.add(1, jsonObjectFunction)
                            ", requestBody = %M(listOfNotNull($it))"
                        } ?: ""

                        val partialArg = partial?.let {
                            ", partial = partial"
                        } ?: ""

                        funcBuilder.addCode(
                            "val response = $httpCallFuncName(\"$funcName\", \"$path\", %T.$httpMethod$parametersArg$bodyArg$partialArg)\n",
                            *references.toTypedArray()
                        )
                        val resultTypes = mutableListOf<TypeName>()
                        endpoint.responseBody?.let {
                            funcBuilder.addStatement(buildString {
                                append("return·")
                                appendType(it, resultTypes, model)
                                append(".deserialize(response)")
                            }, *resultTypes.toTypedArray())
                        }
                    }.build()
                })
            }.build())
        }.build()
    }
}

private fun httpCallFuncNameToMethod(endpoint: HA_Endpoint): Pair<String, String> {
    return when (endpoint.method) {
        HTTP_POST,
        REST_CREATE -> "callWithBody" to "Post"

        HTTP_GET,
        REST_GET,
        REST_QUERY -> "callWithParameters" to "Get"

        HTTP_PATCH,
        REST_UPDATE -> "callWithBody" to "Patch"

        HTTP_DELETE,
        REST_DELETE -> "callWithParameters" to "Delete"

        HTTP_PUT -> "callWithBody" to "Put"
    }
}

private fun parameterConversion(parameter: HA_Parameter, funcBuilder: Builder) {
    @Suppress("UNUSED_VARIABLE")
    val unused: Any? = when (val type = parameter.field.type) {
        is HA_Type.Primitive,
        is HA_Type.UrlParam, // TODO: Support UrlParam
        is HA_Type.Enum -> {
        }

        is HA_Type.Ref -> if (type.nullable) {
            funcBuilder.addStatement("val ${parameter.field.name} = ${parameter.field.name}?.id")
        } else {
            funcBuilder.addStatement("val ${parameter.field.name} = ${parameter.field.name}.id")
        }

        is HA_Type.Array -> {
            if (type.elementType is HA_Type.Ref) {
                funcBuilder.addStatement("val ${parameter.field.name} = ${parameter.field.name}.map·{ it.id }")
            }
            Unit
        }

        is HA_Type.Object,
        is HA_Type.Dto -> error("Objects cannot occur in URL parameters")
    }
}

private fun getFuncParamsAndDeprecationKDoc(
    model: HttpApiEntitiesById,
    urlParams: List<HA_Field>,
    bodyParams: List<HA_Field>?,
    hasUrlBatchInfo: Boolean,
    partialStructure: Pair<String, Array<ClassName>>?,
    partialKP: TypeName? // must have the same nullability as `partialStructure`
): Pair<List<ParameterSpec>, String?> {
    val deprecation = StringBuilder()
    val params = mutableListOf<ParameterSpec>().also { funcParams ->
        fun paramWithDefault(paramField: HA_Field): ParameterSpec {
            val parameter = ParameterSpec.builder(paramField.name, paramField.type.kotlinPoet(model))
            when {
                paramField.type.optional -> parameter.defaultValue("%T", optionNoneType)
                paramField.type.nullable -> parameter.defaultValue("null")
            }
            paramField.deprecation?.run {
                deprecation.append("@param ${paramField.name} deprecated since $since" +
                    ", scheduled for removal".takeIf { forRemoval }.orEmpty() +
                    ". $message\n"
                )
            }
            return parameter.build()
        }

        urlParams.forEach {
            if (it.name.startsWith(META_PARAMETERS_PREFIX)) return@forEach
            funcParams.add(paramWithDefault(it))
        }

        funcParams.addAll(bodyParams.orEmpty().map { paramWithDefault(it) })

        if (hasUrlBatchInfo) {
            funcParams.add(
                ParameterSpec.builder("batchInfo", batchInfoType.copy(nullable = true))
                    .defaultValue("null")
                    .build()
            )
        }

        if (partialStructure != null) {
            funcParams.add(
                ParameterSpec.builder(
                    "buildPartial", LambdaTypeName.get(
                        partialType.parameterizedBy(partialKP!!),
                        listOf(),
                        UNIT
                    )
                ).defaultValue("${partialStructure.first}.defaultPartialFull", *partialStructure.second).build()
            )
        }
    }
    return params to deprecation.toString().takeIf { it.isNotEmpty() }
}

private fun displayPath(it: HA_Resource, model: HttpApiEntitiesById) =
    ancestors(it, model).map(HA_Resource::displayPlural).toList().asReversed()

private fun ancestors(resource: HA_Resource, model: HttpApiEntitiesById): Sequence<HA_Resource> {
    return generateSequence(resource) {
        it.parentResource?.run { model.resources.getValue(id) }
    }
}
