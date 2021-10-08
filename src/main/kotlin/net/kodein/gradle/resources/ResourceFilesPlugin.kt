package net.kodein.gradle.resources

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByName
import org.gradle.language.jvm.tasks.ProcessResources
import javax.inject.Inject


class ResourceFilesPlugin @Inject constructor(
    val scf: SoftwareComponentFactory
) : Plugin<Project> {

    companion object {
        private val resourceFilesAttribute = Attribute.of("net.kodein.gradle.resources.resourceFiles", String::class.java)
        private val resourceFilesAttributeValue = "archive"

        const val filesDirectoryName = "files"
        const val extensionName = "resourceFiles"
        const val importedResourceFilesConfName = "resourceFiles"
        const val exportedResourceFilesConfName = "exportedResourceFiles"
        const val archiveResourceFilesTaskName = "archiveResourceFiles"
        const val importResourceFilesTaskName = "importResourceFiles"
        const val resolveResourceFilesTaskName = "resolveResourceFiles"
        const val componentName = "resourceFiles"
    }

    class Extension(private val project: Project) {
        var outputDir = project.file("${project.buildDir}/resources")

        @Suppress("UnstableApiUsage")
        fun addToProcessResources(taskName: String) {
            project.tasks.getByName<ProcessResources>(taskName) {
                dependsOn("importResourceFiles")
                from(outputDir)
            }
        }
    }

    override fun apply(project: Project) {
        val ext = Extension(project)
        project.extensions.add(extensionName, ext)

        project.dependencies.attributesSchema { attribute(resourceFilesAttribute) }

        val importedResourceFilesConf = project.configurations.create(importedResourceFilesConfName) {
            attributes.attribute(resourceFilesAttribute, resourceFilesAttributeValue)
            isCanBeResolved = true
            isCanBeConsumed = false
        }

        if (project.file(filesDirectoryName).isDirectory) {
            val exportedResourceFilesConf = project.configurations.create(exportedResourceFilesConfName) {
                attributes.attribute(resourceFilesAttribute, resourceFilesAttributeValue)
                extendsFrom(importedResourceFilesConf)
                isCanBeConsumed = true
                isCanBeResolved = false
            }
            project.configurations.maybeCreate("default").extendsFrom(exportedResourceFilesConf)

            val archiveResourceFilesTask = project.tasks.create<Zip>(archiveResourceFilesTaskName) {
                group = "build"
                from(project.file(filesDirectoryName))
                archiveFileName.set("${project.name}.zip")
                destinationDirectory.set(project.file("${project.buildDir}/archives"))
            }

            project.artifacts.add(exportedResourceFilesConfName, archiveResourceFilesTask)

            val component = scf.adhoc(componentName)
            project.components.add(component)
            component.addVariantsFromConfiguration(exportedResourceFilesConf) {
                mapToMavenScope("compile")
            }

            project.tasks.create<Delete>("cleanArchive") {
                group = "build"
                delete(project.file("${project.buildDir}/archives"))
            }

            project.afterEvaluate {
                tasks.maybeCreate("clean").dependsOn("cleanArchive")

                project.extensions.findByType<PublishingExtension>()?.publications?.create<MavenPublication>(componentName) {
                    from(component)
                }
            }

        } else {
            project.afterEvaluate {
                val importResourceFilesTask = project.tasks.create<Copy>(importResourceFilesTaskName) {
                    group = "resources"
                    into(ext.outputDir)
                }
                val resolveResourceFilesTask = project.tasks.create(resolveResourceFilesTaskName) {
                    dependsOn(importedResourceFilesConf)
                    doLast {
                        importedResourceFilesConf.resolve()
                            .forEach {
                                importResourceFilesTask.from(project.zipTree(it))
                            }
                    }
                }
                importResourceFilesTask.dependsOn(resolveResourceFilesTask)

                project.tasks.create<Delete>("cleanResouces") {
                    group = "build"
                    delete(project.file(ext.outputDir))
                }
                tasks.maybeCreate("clean").dependsOn("cleanResouces")
            }
        }
    }
}
