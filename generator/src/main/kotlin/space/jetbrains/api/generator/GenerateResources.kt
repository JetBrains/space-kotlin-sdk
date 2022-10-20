package space.jetbrains.api.generator

import app.cash.exhaustive.Exhaustive
import com.squareup.kotlinpoet.*
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

            if (displayPath.size == 1) { // top-level resource
                fileBuilder.addProperty(
                    PropertySpec.builder(resourceGroup.first().displayPlural.displayNameToMemberName(), className)
                        .receiver(clientType)
                        .getter(FunSpec.getterBuilder().addStatement("return %T(this)", className).build())
                        .build()
                )
            }

            fileBuilder.addType(TypeSpec.classBuilder(className).also { typeBuilder ->
                typeBuilder.primaryConstructor(FunSpec.constructorBuilder().addParameter("client", clientType).build())
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
                    val queryParams = endpoint.parameters.filter { !it.path }.map { it.field }
                    val bodyParams = endpoint.requestBody?.fields
                    val returnType = endpoint.responseBody?.kotlinPoet(model)
                    val (partial, batch) = endpoint.responseBody.partial()
                    val partialInterface = partial?.partialToPartialInterface(model)
                    val hasUrlBatchInfo = queryParams.any {
                        it.name == META_PARAMETERS_PREFIX + "skip" || it.name == META_PARAMETERS_PREFIX + "top"
                    }

                    val funcParams = getFuncParams(model, urlParams, bodyParams, hasUrlBatchInfo, partialInterface)

                    val fullPath = endpoint.path.segments.asReversed().plus(
                        ancestors(model.resources.getValue(endpoint.resource.id), model)
                            .flatMap { it.path.segments.asReversed().asSequence() }
                    ).asReversed()

                    FunSpec.builder(endpoint.functionName).also { funcBuilder ->
                        val kDoc = buildString {
                            buildKDoc(endpoint.description)?.let {
                                append(it)
                            }
                            funcParams.mapNotNull { it.second }.joinToString("").takeUnless { it.isBlank() }?.let {
                                append(it)
                            }
                        }

                        kDoc.takeUnless { it.isBlank() }?.let { funcBuilder.addKdoc(it) }
                        funcBuilder.annotations.deprecation(endpoint.deprecation)
                        funcBuilder.annotations.experimental(endpoint.experimental)
                        funcBuilder.addModifiers(KModifier.SUSPEND)
                        funcBuilder.addParameters(funcParams.map { it.first })
                        if (returnType != null) funcBuilder.returns(returnType)

                        funcBuilder.addCode(CodeBlock.builder().also { code ->
                            if (partialInterface != null) {
                                code.add("val partial = %T(", partialBuilderType)
                                if (batch) {
                                    code.add("isBatch = true")
                                }
                                code.add(").also·{\n")
                                code.indent()
                                code.partialImplConstructor(partialInterface, "it")
                                code.add(".apply(buildPartial)\n")
                                code.unindent()
                                code.add("}\n")
                            }

                            val (httpCallFuncName, httpMethod) = httpCallFuncNameToMethod(endpoint)

                            val pathParams = endpoint.parameters.filter { it.path }.associateBy { it.field.name }
                            code.add("val response = $httpCallFuncName(%S, \"", endpoint.functionName)
                            val pathIterator = fullPath.iterator()
                            pathIterator.forEach { segment ->
                                fun pathParam(name: String): CodeBlock.Builder {
                                    code.add("\${pathParam(")
                                    val paramType = pathParams.getValue(name).field.type
                                    if (paramType is HA_Type.UrlParam) {
                                        code.add("$name.compactId")
                                    } else {
                                        code.add(name)
                                    }
                                    return code.add(")}")
                                }
                                when (segment) {
                                    is Const -> code.add(segment.value)
                                    is Var -> pathParam(segment.name)
                                    is PrefixedVar -> {
                                        code.add(segment.prefix + ":")
                                        pathParam(segment.name)
                                    }
                                }.let {}
                                if (pathIterator.hasNext()) code.add("/")
                            }
                            code.add("\", %T.$httpMethod", httpMethodType)

                            if (queryParams.isNotEmpty()) {
                                code.add(", parameters = %T.build·{\n", parametersType)
                                code.indent()
                                if (hasUrlBatchInfo) {
                                    code.addStatement("appendBatchInfo(batchInfo)")
                                }
                                queryParams.forEach { param ->
                                    val name = param.name
                                    if (name.startsWith(META_PARAMETERS_PREFIX)) return@forEach

                                    when {
                                        param.type is HA_Type.Array -> {
                                            parameterConversion(model, param.name, param.funcParameterHaType(), code)
                                            code.add("?.let·{ appendAll(%S, it) }\n", param.name)
                                        }
                                        param.requiresAddedNullability || param.defaultValue == HA_DefaultValue.NULL -> {
                                            code.add(param.name + "?.let·{ append(%S, ", param.name)
                                            parameterConversion(model, "it", param.type.copy(nullable = false), code)
                                            code.add(") }\n")
                                        }
                                        param.type.nullable -> {
                                            code.add("append(%S, ${param.name}?.let·{ ", param.name)
                                            parameterConversion(model, "it", param.type.copy(nullable = false), code)
                                            code.add(" }.orEmpty())\n")
                                        }
                                        else -> {
                                            code.add("append(%S, ", param.name)
                                            parameterConversion(model, param.name, param.type, code)
                                            code.add(")\n")
                                        }
                                    }
                                }
                                code.unindent()
                                code.add("}")
                            }

                            if (endpoint.requestBody != null) {
                                code.add(", requestBody = %M(listOfNotNull(\n", jsonObjectFunction)
                                code.indent()
                                val fieldIterator = endpoint.requestBody.fields.iterator()
                                while (fieldIterator.hasNext()) {
                                    val field = fieldIterator.next()
                                    fun serialize(expr: String = field.name) {
                                        if (WEBHOOK_PAYLOAD_FIELDS_TAG in field.type.tags) {
                                            code.appendType(field.type, model, field.requiresOption)
                                            code.add(".serialize(%M($expr))", webhookPayloadFieldsPartialFunction)
                                        } else if (PERMISSION_SCOPE_TAG in field.type.tags) {
                                            code.appendType(field.type, model, field.requiresOption)
                                            code.add(".serialize(%M($expr))", permissionScopeToStringFunction)
                                        } else {
                                            code.appendType(field.type, model, field.requiresOption)
                                            code.add(".serialize($expr)")
                                        }
                                    }
                                    when {
                                        field.requiresOption -> {
                                            serialize()
                                            code.add("?.let·{ %S·to it }", field.name)
                                        }
                                        field.requiresAddedNullability || field.defaultValue == HA_DefaultValue.NULL -> {
                                            code.add("${field.name}?.let·{ %S·to ", field.name)
                                            serialize("it")
                                            code.add(" }")
                                        }
                                        else -> {
                                            code.add("%S·to ", field.name)
                                            serialize()
                                        }
                                    }
                                    if (fieldIterator.hasNext()) code.add(",")
                                    code.add("\n")
                                }
                                code.unindent()
                                code.add("))")
                            }

                            if (endpoint.returnsSyncBatch()) {
                                code.add(", requestHeaders = listOfNotNull(getSyncEpochHeader())")
                            }

                            if (partial != null) {
                                code.add(", partial = partial")
                            }

                            code.add(")\n")

                            if (endpoint.responseBody != null) {
                                code.add("return·")
                                code.appendType(endpoint.responseBody, model, false)
                                code.add(".deserialize(response)")
                            }
                        }.build())
                    }.build()
                })
            }.build())
        }.build()
    }
}

val HA_Field.requiresOption get() = optional && defaultValue == null && type.nullable

private val HA_Field.requiresAddedNullability get() = optional && defaultValue == null && !type.nullable

private fun HA_Field.funcParameterHaType() = type.let {
    if (requiresAddedNullability) it.copy(nullable = true) else it
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

// String -> String
// String? -> String?
// <primitive>? -> String
// <URL param>!! -> String
// Enum!! -> String
// <Ref> -> String
// <Ref>? -> String?
// List<T>? -> List<String>?
// PermissionScope -> String
// PermissionScope? -> String?
fun parameterConversion(model: HttpApiEntitiesById, expr: String, type: HA_Type, funcCode: CodeBlock.Builder) {
    @Exhaustive
    when (type) {
        is HA_Type.Primitive -> {
            funcCode.add(expr)
            if (type.primitive != HA_Primitive.String) {
                funcCode.add(".toString()")
            } else if (PERMISSION_SCOPE_TAG in type.tags) {
                if (type.nullable) funcCode.add("?")
                funcCode.add(".toString()")
            }
        }
        is HA_Type.UrlParam -> funcCode.add("$expr.compactId")
        is HA_Type.Enum -> funcCode.add("$expr.name")

        is HA_Type.Ref -> funcCode.add(expr + if (type.nullable) "?.id" else ".id")

        is HA_Type.Array -> {
            funcCode.add(expr)
            if (type.nullable) funcCode.add("?")
            funcCode.add(".takeIf·{ it.isNotEmpty() }")
            if (type.elementType !is HA_Type.Primitive || type.elementType.primitive != HA_Primitive.String || type.elementType.nullable) {
                funcCode.add("?.map·{ ")
                if (type.elementType.nullable) {
                    funcCode.add("it?.let·{ ")
                    parameterConversion(model, "it", type.elementType, funcCode)
                    funcCode.add(" }.orEmpty()")
                } else {
                    parameterConversion(model, "it", type.elementType, funcCode)
                }
                funcCode.add(" }")
            }
        }

        is HA_Type.Map -> error("Maps cannot occur in URL parameters")

        is HA_Type.Object -> error("Objects cannot occur in URL parameters")
        is HA_Type.Dto -> error("${type.dto.id}: DTOs cannot occur as URL parameters or as fields inside @HttpApiUrlParam classes")
    }
}

fun CodeBlock.Builder.default(type: HA_Type, model: HttpApiEntitiesById, defaultValue: HA_DefaultValue) {
    when (defaultValue) {
        is HA_DefaultValue.Const.Primitive -> add(defaultValue.expression)
        is HA_DefaultValue.Const.EnumEntry -> {
            type as HA_Type.Enum
            val className = ClassName(TYPES_PACKAGE, model.enums.getValue(type.enum.id).name.kotlinClassNameJoined())
            add("%T." + defaultValue.entryName, className)
        }
        is HA_DefaultValue.Collection -> {
            add("listOf(")
            val elements = defaultValue.elements.iterator()
            elements.forEach {
                default((type as HA_Type.Array).elementType, model, it)
                if (elements.hasNext()) add(", ")
            }
            add(")")
        }
        is HA_DefaultValue.Map -> {
            add("mapOf(")
            val elements = defaultValue.elements.iterator()
            elements.forEach {
                add("%S·to ", it.key)
                default((type as HA_Type.Map).valueType, model, it.value)
                if (elements.hasNext()) add(", ")
            }
            add(")")
        }
        is HA_DefaultValue.Reference -> add(defaultValue.paramName)
    }.let {}
}

private fun getFuncParams(
    model: HttpApiEntitiesById,
    urlParams: List<HA_Field>,
    bodyParams: List<HA_Field>?,
    hasUrlBatchInfo: Boolean,
    partialInterface: TypeName?
): List<Pair<ParameterSpec, String?>> {

    fun paramWithDefault(paramField: HA_Field): Pair<ParameterSpec, String?> {
        val type = when {
            WEBHOOK_PAYLOAD_FIELDS_TAG in paramField.type.tags -> {
                LambdaTypeName.get(receiver = webhookEventPartialType, returnType = UNIT)
                    .copy(nullable = paramField.type.nullable, option = paramField.requiresOption)
            }
            PERMISSION_SCOPE_TAG in paramField.type.tags -> {
                permissionScopeType.copy(nullable = paramField.type.nullable, option = paramField.requiresOption)
            }
            else -> paramField.funcParameterHaType().kotlinPoet(model, paramField.requiresOption)
        }
        val parameter = ParameterSpec.builder(
            paramField.name,
            type
        )
        when {
            paramField.requiresOption -> parameter.defaultValue("%T", optionNoneType)
            paramField.requiresAddedNullability -> parameter.defaultValue("null")
            paramField.defaultValue != null -> parameter.defaultValue(buildCodeBlock {
                default(paramField.type, model, paramField.defaultValue)
            })
        }
        return parameter.build() to paramDescription(paramField)
    }

    val params = mutableListOf<Pair<ParameterSpec, String?>>().also { funcParams ->
        urlParams.forEach {
            if (!it.name.startsWith(META_PARAMETERS_PREFIX)) {
                funcParams.add(paramWithDefault(it))
            }
        }

        bodyParams?.forEach {
            funcParams.add(paramWithDefault(it))
        }

        if (hasUrlBatchInfo) {
            funcParams.add(
                ParameterSpec.builder("batchInfo", batchInfoType.copy(nullable = true))
                    .defaultValue("null")
                    .build() to null
            )
        }

        if (partialInterface != null) {
            funcParams.add(
                ParameterSpec.builder(
                    name = "buildPartial",
                    type = LambdaTypeName.get(
                        receiver = partialInterface,
                        returnType = UNIT
                    )
                ).defaultValue("%T::defaultPartial", partialType).build() to null
            )
        }
    }
    return params
}

private fun displayPath(it: HA_Resource, model: HttpApiEntitiesById) =
    ancestors(it, model).map(HA_Resource::displayPlural).toList().asReversed()

private fun ancestors(resource: HA_Resource, model: HttpApiEntitiesById): Sequence<HA_Resource> {
    return generateSequence(resource) {
        it.parentResource?.run { model.resources.getValue(id) }
    }
}

private fun HA_Endpoint.returnsSyncBatch() = this.responseBody is HA_Type.Object && this.responseBody.kind == HA_Type.Object.Kind.SYNC_BATCH