/**
* JetBrains Space Automation
* This Kotlin-script file lets you automate build activities
* For more info, refer to https://www.jetbrains.com/help/space/automation.html
*/

job("Hello World!") {
    container("hello-world")
}

job(name = "Build") { gradlew("openjdk:11", "build") }
