dependencyResolutionManagement {
    repositories {
        mavenCentral()

        maven("https://plugins.gradle.org/m2/")
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2/")

        // repositories required for Kotlin/JS not present here, see https://youtrack.jetbrains.com/issue/KT-51379
        // TODO: set RepositoriesMode.FAIL_ON_PROJECT_REPOS at some point
    }
}

include("runtime")
include("generator")
