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
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import javax.inject.Inject


class ResourceFilesPlugin @Inject constructor(
    val scf: SoftwareComponentFactory
) : Plugin<Project> {

    companion object {
        private val resourceFilesAttribute = Attribute.of("net.kodein.gradle.resources.resourceFiles", String::class.java)
        private val resourceFilesAttributeValue = "archive"

        const val extensionName = "resourceFiles"
        const val importedResourceFilesConfName = "resourceFiles"
        const val exportedResourceFilesConfName = "exportedResourceFiles"
        const val archiveResourceFilesTaskName = "archiveResourceFiles"
        const val importResourceFilesTaskName = "importResourceFiles"
        const val resolveResourceFilesTaskName = "resolveResourceFiles"
        const val componentName = "resourceFiles"
    }

    class Extension(private val project: Project) {
        var inputDirs: List<File> = listOf(project.file("${project.projectDir}/files"))
        var outputDir: File = project.file("${project.buildDir}/resources")

        @Suppress("UnstableApiUsage")
        fun addToProcessResources(taskName: String) {
            project.tasks.named<ProcessResources>(taskName) {
                dependsOn("importResourceFiles")
                from(outputDir)
            }
        }
    }

    override fun apply(project: Project) {
        project.plugins.apply("base")

        val ext = Extension(project)
        project.extensions.add(extensionName, ext)

        project.dependencies.attributesSchema { attribute(resourceFilesAttribute) }

        val importedResourceFilesConf = project.configurations.register(importedResourceFilesConfName) {
            attributes.attribute(resourceFilesAttribute, resourceFilesAttributeValue)
            isCanBeResolved = true
            isCanBeConsumed = false
        }

        if (ext.inputDirs.any { it.isDirectory }) {
            val exportedResourceFilesConf = project.configurations.create(exportedResourceFilesConfName) {
                attributes.attribute(resourceFilesAttribute, resourceFilesAttributeValue)
                extendsFrom(importedResourceFilesConf.get())
                isCanBeConsumed = true
                isCanBeResolved = false
            }
            project.configurations.named("default").configure { extendsFrom(exportedResourceFilesConf) }

            val archiveResourceFilesTask = project.tasks.register<Zip>(archiveResourceFilesTaskName) {
                group = "build"
                from(*ext.inputDirs.toTypedArray())
                archiveFileName.set("${project.name}.zip")
                destinationDirectory.set(project.file("${project.buildDir}/archives"))
            }

            project.artifacts.add(exportedResourceFilesConfName, archiveResourceFilesTask)

            val component = scf.adhoc(componentName)
            project.components.add(component)
            component.addVariantsFromConfiguration(exportedResourceFilesConf) {
                mapToMavenScope("compile")
            }

            project.tasks.named<Delete>("clean").configure {
                delete(project.file("${project.buildDir}/archives"))
            }

            project.afterEvaluate {
                project.extensions.findByType<PublishingExtension>()?.publications?.register<MavenPublication>(componentName) {
                    from(component)
                }
            }

        } else {
            val importResourceFilesTask = project.tasks.register<Copy>(importResourceFilesTaskName) {
                group = "resources"
                into(ext.outputDir)
            }
            val resolveResourceFilesTask = project.tasks.register(resolveResourceFilesTaskName) {
                dependsOn(importedResourceFilesConf)
                doLast {
                    importedResourceFilesConf.get().resolve()
                        .forEach {
                            importResourceFilesTask.configure { from(project.zipTree(it)) }
                        }
                }
            }
            importResourceFilesTask.configure { dependsOn(resolveResourceFilesTask) }

            project.tasks.named<Delete>("clean").configure {
                delete(project.file(ext.outputDir))
            }
        }
    }
}
