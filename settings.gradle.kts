dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()

        maven("https://plugins.gradle.org/m2/")
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2/")
    }
}

include("runtime")
include("generator")
