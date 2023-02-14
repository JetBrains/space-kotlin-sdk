plugins {
    id("org.jetbrains.kotlin.jvm")
    id("application")
    id("maven-publish")
}

val mavenForPublishing: (PublishingExtension) -> Unit by rootProject.extra
val kotlin_version: String by rootProject.extra
val jackson_version: String by rootProject.extra
val this_version: String by rootProject.extra

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-core:$jackson_version")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:$jackson_version")

    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    api("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")

    api("com.squareup:kotlinpoet:1.4.4")
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("space.jetbrains.api.generator.MainKt")
}

publishing {
    mavenForPublishing(this)
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.jetbrains"
            artifactId = "space-sdk-generator"
            version = this_version
            from(components["java"])

            pom {
                name.set("Space SDK generator")
                description.set("SDK generator for JetBrains Space API")
            }
        }
    }
}
