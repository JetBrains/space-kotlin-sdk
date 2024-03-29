@file:Suppress("PropertyName")

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

val this_version: String by rootProject.extra
val mavenForPublishing: (PublishingExtension) -> Unit by rootProject.extra
val ktor_version: String by rootProject.extra
val kotlinx_coroutines_version: String by rootProject.extra
val kotlin_version: String by rootProject.extra
val jackson_version: String by rootProject.extra
val assertk_version: String by rootProject.extra

group = "org.jetbrains"
version = this_version

kotlin {
    explicitApi()
    jvm {
        compilations.configureEach {
            tasks.named(compileKotlinTaskName).configure {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        getByName("commonMain") {
            dependencies {
                api("io.ktor:ktor-client-core:$ktor_version")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            }
        }

        getByName("jvmMain") {
            dependencies {
                api("com.fasterxml.jackson.core:jackson-core:$jackson_version")
                api("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
            }
        }

        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation("com.willowtreeapps.assertk:assertk-jvm:$assertk_version")

                implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
                implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
                implementation("io.ktor:ktor-client-apache:$ktor_version")
                implementation("ch.qos.logback:logback-classic:1.4.1")
            }
        }
    }
}

publishing {
    mavenForPublishing(this)
    publications {
        withType(MavenPublication::class) {
            artifactId = "space-sdk-$artifactId"
            pom {
                name.set("Space SDK runtime")
                description.set("Runtime for JetBrains Space SDK")
            }
        }
    }
}
