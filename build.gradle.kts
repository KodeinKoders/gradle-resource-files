plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "net.kodein.gradle.resources"
version = "1.0.0"

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins.create("resource-files") {
        id = "net.kodein.gradle.resources.resource-files"
        implementationClass = "net.kodein.gradle.resources.ResourceFilesPlugin"
    }
}
