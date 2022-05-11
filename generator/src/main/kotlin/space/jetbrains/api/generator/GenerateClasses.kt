package space.jetbrains.api.generator

import com.squareup.kotlinpoet.*

fun generateClasses(model: HttpApiEntitiesById): List<FileSpec> {
    Log.info { "Generating Class classes for SDK" }

    return model.classes.map { clazz ->
        Log.info { "Generating file with Class classes for '${clazz.name}' and its subclasses" }

        val className = ClassName(TYPES_PACKAGE, clazz.name.kotlinClassName())

        FileSpec.builder(className.packageName, className.simpleNames.joinToString("")).also { fileBuilder ->
            fileBuilder.indent(INDENT)

            val type = buildType(clazz, className, model)

            fileBuilder.addType(type)
        }.build()
    }
}

private fun buildType(
    clazz: HA_Class,
    className: ClassName,
    model: HttpApiEntitiesById
): TypeSpec {
    val typeBuilder = when (clazz) {
        is HA_Class.Clazz -> TypeSpec.classBuilder(className).apply {
            if (clazz.open) {
                addModifiers(KModifier.OPEN)
            }
            if (clazz.abstract) {
                addModifiers(KModifier.ABSTRACT)
            }
        }
        is HA_Class.Interface -> TypeSpec.interfaceBuilder(className).apply {
            if (clazz.sealed) {
                addModifiers(KModifier.SEALED)
            }
        }
        is HA_Class.Object -> TypeSpec.objectBuilder(className)
    }

    clazz.doc?.takeIf { it.isNotBlank() }?.also { typeBuilder.addKdoc(CodeBlock.of(it)) }

    typeBuilder.addProperties(
        clazz.properties.map {
            val propBuilder = PropertySpec.builder(it.name, it.type.kotlinPoet(model, false))
            propBuilder.annotations.deprecation(it.deprecation)
            when (it.visibilityModifier) {
                HA_VisibilityModifier.DEFAULT -> {}
                HA_VisibilityModifier.PRIVATE -> typeBuilder.addModifiers(KModifier.PRIVATE)
                HA_VisibilityModifier.INTERNAL -> typeBuilder.addModifiers(KModifier.INTERNAL)
            }
            if (it.value != null) {
                propBuilder.initializer(
                    CodeBlock.builder().apply { default(it.type, model, it.value) }.build()
                )
            }
            if (it.override) {
                propBuilder.addModifiers(KModifier.OVERRIDE)
            }
            propBuilder.build()
        }
    )

    clazz.innerSubclasses.forEach {
        typeBuilder.addType(buildType(it, ClassName(TYPES_PACKAGE, it.name.kotlinClassName()), model))
    }

    typeBuilder.annotationSpecs.deprecation(clazz.deprecation)

    when (clazz.visibilityModifier) {
        HA_VisibilityModifier.DEFAULT -> {}
        HA_VisibilityModifier.PRIVATE -> typeBuilder.addModifiers(KModifier.PRIVATE)
        HA_VisibilityModifier.INTERNAL -> typeBuilder.addModifiers(KModifier.INTERNAL)
    }

    clazz.implements.forEach {
        typeBuilder.addSuperinterface(ClassName(TYPES_PACKAGE, it.kotlinClassName()))
    }

    return typeBuilder.build()
}