plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.18.0"
}

group = "net.kodein.gradle.resources"
version = "1.0.0"

repositories {
    mavenCentral()
}

System.getenv("GRADLE_PUBLISH_KEY")?.let { ext["gradle.publish.key"] = it }
System.getenv("GRADLE_PUBLISH_SECRET")?.let { ext["gradle.publish.secret"] = it }

gradlePlugin {
    plugins.create("resource-files") {
        id = "net.kodein.gradle.resources.resource-files"
        implementationClass = "net.kodein.gradle.resources.ResourceFilesPlugin"
        displayName = "Resource files dependencies"
        description = "Gradle Plugin to distribute resource files archives"
    }
}

pluginBundle {
    website = "https://github.com/KodeinKoders/gradle-resource-files"
    vcsUrl = "https://github.com/KodeinKoders/gradle-resource-files.git"
    description = "Gradle Plugin to distribute resource files archives"
    tags = listOf("resources", "dependencies")
}
