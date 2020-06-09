package space.jetbrains.api.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import space.jetbrains.api.generator.HA_Type.Object.Kind.*
import space.jetbrains.api.generator.HA_Type.Object.Kind.MAP_ENTRY

fun generatePartials(model: HttpApiEntitiesById): List<FileSpec> {
    return model.dtoAndUrlParams.values.mapNotNull { root ->
        if (root.extends != null) return@mapNotNull null

        val rootClassName = root.getClassName()
        if (rootClassName == batchInfoType) return@mapNotNull null

        val rootPartialClassName = rootClassName.dtoToPartialInterface()

        FileSpec.builder(rootPartialClassName.packageName, rootPartialClassName.simpleName).also { file ->
            file.indent(INDENT)

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

                dto.fields.forEach {
                    val type = it.field.type
                    val (partial, specialPartial) = type.partial()

                    val interfaceFunBuilder = FunSpec.builder(it.field.name).addModifiers(ABSTRACT)
                    val implFunBuilder = FunSpec.builder(it.field.name).addModifiers(OVERRIDE)

                    if (partial == null) {
                        implFunBuilder.addStatement("builder.add(%S)", it.field.name)
                    } else {
                        interfaceBuilder.addFunction(
                            FunSpec.builder(it.field.name)
                                .addModifiers(ABSTRACT)
                                .addParameter("recursiveAs", dtoPartialClassName)
                                .build()
                        )
                        implBuilder.addFunction(
                            FunSpec.builder(it.field.name)
                                .addModifiers(OVERRIDE)
                                .addParameter("recursiveAs", dtoPartialClassName)
                                .addStatement("builder.addRecursively(%S, (recursiveAs as %T).builder)", it.field.name, dtoPartialImplClassName)
                                .build()
                        )

                        val partialInterfaceOrNothing = partial.getPartialInterfaceOrNothing(model)

                        val buildPartialParameter = ParameterSpec.builder(
                            name = "build",
                            type = LambdaTypeName.get(receiver = partialInterfaceOrNothing, returnType = UNIT)
                        ).build()
                        interfaceFunBuilder.addParameter(
                            buildPartialParameter.toBuilder().defaultValue("%T::defaultPartial", partialType).build()
                        )
                        implFunBuilder.addParameter(buildPartialParameter)

                        implFunBuilder.addCode(buildCodeBlock {
                            add("builder.add(%S, {\n", it.field.name)
                            indent()
                            partialImplConstructor(partialInterfaceOrNothing, "it")
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

                    interfaceBuilder.addFunction(interfaceFunBuilder.build())
                    implBuilder.addFunction(implFunBuilder.build())
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

fun HA_Type.getPartialInterface(model: HttpApiEntitiesById): TypeName? = when (this) {
    is HA_Type.Primitive -> null
    is HA_Type.Array -> elementType.getPartialInterface(model)
    is HA_Type.Object -> when (kind) {
        PAIR -> apiPairPartialType.parameterizedBy(
            firstType().getPartialInterfaceOrNothing(model),
            secondType().getPartialInterfaceOrNothing(model)
        )
        TRIPLE -> apiTriplePartialType.parameterizedBy(
            firstType().getPartialInterfaceOrNothing(model),
            secondType().getPartialInterfaceOrNothing(model),
            thirdType().getPartialInterfaceOrNothing(model)
        )
        MAP_ENTRY -> apiMapEntryPartialType.parameterizedBy(
            keyType().getPartialInterfaceOrNothing(model),
            valueType().getPartialInterfaceOrNothing(model)
        )
        BATCH -> batchDataType().getPartialInterface(model)
        MOD -> modPartialType.parameterizedBy(modSubjectType().getPartialInterfaceOrNothing(model))
        REQUEST_BODY -> error("Request bodies do not have partials")
    }
    is HA_Type.Dto -> model.dtoAndUrlParams.getValue(dto.id).getClassName().dtoToPartialInterface()
    is HA_Type.Ref -> model.dtoAndUrlParams.getValue(dto.id).getClassName().dtoToPartialInterface()
    is HA_Type.Enum -> null
    is HA_Type.UrlParam -> model.dtoAndUrlParams.getValue(urlParam.id).getClassName().dtoToPartialInterface()
}

fun HA_Type.getPartialInterfaceOrNothing(model: HttpApiEntitiesById): TypeName = getPartialInterface(model) ?: NOTHING

