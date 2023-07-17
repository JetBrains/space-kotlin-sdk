package space.jetbrains.api.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec

fun HA_FeatureFlag.annotationClassName(): ClassName = ClassName(FF_PACKAGE, displayName.displayNameToClassName())

fun generateFeatureFlags(model: HttpApiEntitiesById): FileSpec {
    return FileSpec.builder(FF_PACKAGE, "FeatureFlags").also { file ->
        model.featureFlags.values.forEach { featureFlag ->
            file.addType(TypeSpec.annotationBuilder(featureFlag.annotationClassName()).also { annotation ->
                annotation.addAnnotation(AnnotationSpec.builder(ClassName("kotlin", "RequiresOptIn")).also { reqOptIn ->
                    reqOptIn.addMember(
                        format = "%S",
                        "This declaration is related to an experimental feature \"${featureFlag.displayName}\". " +
                            "It may be disabled for your organization. Please contact JetBrains support if you " +
                            "would like to enable the feature for your organization.",
                    )
                    reqOptIn.addMember(
                        format = "%T.${RequiresOptIn.Level.ERROR.name}",
                        RequiresOptIn.Level::class,
                    )
                }.build())
            }.build())
        }
    }.build()
}
