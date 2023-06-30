buildscript {
    val this_version: String by extra("0.3.4")

    val kotlin_version: String by extra("1.8.10")
    val jackson_version: String by extra("2.13.1")
    val kotlinx_coroutines_version: String by extra("1.6.4")
    val ktor_version: String by extra("2.2.1")
    val assertk_version: String by extra("0.25")

    val extraProperties = rootProject.extensions.extraProperties

    val jbTeamMavenRepoUsername = System.getenv("JB_TEAM_MAVEN_REPO_USERNAME") ?: if (extraProperties.has("jbTeamMavenRepoUsername")) extraProperties["jbTeamMavenRepoUsername"] as? String else null
    val jbTeamMavenRepoPassword = System.getenv("JB_TEAM_MAVEN_REPO_PASSWORD") ?: if (extraProperties.has("jbTeamMavenRepoPassword")) extraProperties["jbTeamMavenRepoPassword"] as? String else null

    val publicJbSpaceMavenRepoUsername = System.getenv("PUBLIC_JB_SPACE_MAVEN_REPO_USERNAME") ?: if (extraProperties.has("publicJbSpaceMavenRepoUsername")) extraProperties["publicJbSpaceMavenRepoUsername"] as? String else null
    val publicJbSpaceMavenRepoPassword = System.getenv("PUBLIC_JB_SPACE_MAVEN_REPO_PASSWORD") ?: if (extraProperties.has("publicJbSpaceMavenRepoPassword")) extraProperties["publicJbSpaceMavenRepoPassword"] as? String else null

    val mavenForPublishing: (PublishingExtension) -> Unit by extra { publishing: PublishingExtension ->
        if (jbTeamMavenRepoUsername == null) {
            logger.info("jbTeamMavenRepoUsername not defined, skipping publishing setup")
            return@extra
        }
        if (jbTeamMavenRepoPassword == null) {
            logger.info("jbTeamMavenRepoPassword not defined, skipping publishing setup")
            return@extra
        }
        if (publicJbSpaceMavenRepoUsername == null) {
            logger.info("publicJbSpaceMavenRepoUsername not defined, skipping publishing setup")
            return@extra
        }
        if (publicJbSpaceMavenRepoPassword == null) {
            logger.info("publicJbSpaceMavenRepoPassword not defined, skipping publishing setup")
            return@extra
        }

        publishing.repositories.maven("https://packages.jetbrains.team/maven/p/crl/maven") {
            credentials {
                username = jbTeamMavenRepoUsername
                password = jbTeamMavenRepoPassword
            }
        }

        publishing.repositories.maven("https://maven.pkg.jetbrains.space/public/p/space-sdk/maven") {
            credentials {
                username = publicJbSpaceMavenRepoUsername
                password = publicJbSpaceMavenRepoPassword
            }
        }
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.10" apply false
    id("org.jetbrains.kotlin.multiplatform") version "1.8.10" apply false
}
