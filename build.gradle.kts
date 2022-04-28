buildscript {
    val this_version: String by extra("0.2.4-ktor-2.0.0")

    val kotlin_version: String by extra("1.5.10")
    val jackson_version: String by extra("2.13.1")
    val kotlinx_coroutines_version: String by extra("1.5.0")
    val ktor_version: String by extra("2.0.0")
    val assertk_version: String by extra("0.25")

    val mavenForPublishing: (PublishingExtension) -> Unit by extra { publishing: PublishingExtension ->
        val props = rootProject.extensions.extraProperties
        if (!props.has("publicJbSpaceUsername") || !props.has("publicJbSpacePassword")) {
            return@extra
        }

        publishing.repositories.maven("https://packages.jetbrains.team/maven/p/crl/maven") {
            credentials {
                val spaceUsername = "spaceUsername"
                val spacePassword = "spacePassword"
                username = if (props.has(spaceUsername)) props[spaceUsername] as String else System.getenv("JB_SPACE_CLIENT_ID")
                password = if (props.has(spacePassword)) props[spacePassword] as String else System.getenv("JB_SPACE_CLIENT_SECRET")
            }
        }
        publishing.repositories.maven("https://maven.pkg.jetbrains.space/public/p/space-sdk/maven") {
            credentials {
                username = props["publicJbSpaceUsername"] as String
                password = props["publicJbSpacePassword"] as String
            }
        }
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.0" apply false
    id("org.jetbrains.kotlin.multiplatform") version "1.5.0" apply false
}
