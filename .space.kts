job("space-api-client: build and publish") {
    startOn {
        gitPush { enabled = false }
    }
}

fun Job.buildAndZip() {
    container("gradle:7.6.0-jdk17") {
        env["JB_TEAM_MAVEN_REPO_USERNAME"] = Params("jb-team-maven-repo-write-username")
        env["JB_TEAM_MAVEN_REPO_PASSWORD"] = Secrets("jb-team-maven-repo-write-password")

        env["PUBLIC_JB_SPACE_MAVEN_REPO_USERNAME"] = Params("public-jb-space-maven-repo-write-username")
        env["PUBLIC_JB_SPACE_MAVEN_REPO_PASSWORD"] = Secrets("public-jb-space-maven-repo-write-password")

        kotlinScript { api ->
            api.gradlew("publish")
        }
    }
}