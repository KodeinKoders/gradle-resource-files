/*
This is an attempt to have a resource artifact published as a second artifact alongside a Kotlin project.
This does not work.
It is full of weird tricks, and one is missing: see https://discuss.gradle.org/t/projectdependency-artifact-coordinates/41156
*/

//package net.kodein.gradle.resources
//
//import org.gradle.api.Plugin
//import org.gradle.api.Project
//import org.gradle.api.artifacts.Dependency
//import org.gradle.api.artifacts.ExternalModuleDependency
//import org.gradle.api.artifacts.ProjectDependency
//import org.gradle.api.attributes.Attribute
//import org.gradle.api.component.SoftwareComponentFactory
//import org.gradle.api.publish.PublishingExtension
//import org.gradle.api.publish.maven.MavenDependency
//import org.gradle.api.publish.maven.MavenPublication
//import org.gradle.api.publish.maven.internal.dependencies.DefaultMavenProjectDependency
//import org.gradle.api.publish.maven.internal.dependencies.MavenDependencyInternal
//import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
//import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
//import org.gradle.api.tasks.bundling.Zip
//import org.gradle.kotlin.dsl.create
//import org.gradle.kotlin.dsl.findByType
//import org.gradle.kotlin.dsl.get
//import org.gradle.kotlin.dsl.withType
//import java.io.File
//import javax.inject.Inject
//
//
//private val resourceFilesAttribute = Attribute.of("net.kodein.gradle.resources.resourceFiles", String::class.java)
//
//class ResourceFilesPlugin @Inject constructor(
//    val scf: SoftwareComponentFactory
//) : Plugin<Project> {
//
//    class Extension(private val project: Project) {
//        var dir: File = project.file("src/main/resources")
//        val archiveTask: Zip get() = project.tasks["archiveResourceFiles"] as Zip
//        fun <D : Dependency?> D.withResources(): D {
//            if (this != null) project.configurations["resourceFiles"].dependencies.add(this)
//            return this
//        }
//    }
//
//    class TransformedDependency(private val delegate: DefaultMavenProjectDependency) : MavenDependencyInternal by delegate {
//        override fun getArtifactId(): String = "${delegate.artifactId}-resource-files"
//    }
//
//    override fun apply(project: Project) {
//        val ext = Extension(project)
//        project.extensions.add("resourceFiles", ext)
//
//        project.dependencies.attributesSchema { attribute(resourceFilesAttribute) }
//
//        val resourceFilesConf = project.configurations.create("resourceFiles") {
//            attributes.attribute(resourceFilesAttribute, "archive")
//            isCanBeResolved = false
//            isCanBeConsumed = false
//        }
//
//        val importedResourceFilesConf = project.configurations.create("importedResourceFiles") {
//            attributes.attribute(resourceFilesAttribute, "archive")
//            isCanBeResolved = true
//            isCanBeConsumed = false
//        }
//
//        val exportedResourceFilesConf = project.configurations.create("exportedResourceFiles") {
//            attributes.attribute(resourceFilesAttribute, "archive")
//            extendsFrom(importedResourceFilesConf)
//            isCanBeConsumed = true
//            isCanBeResolved = false
//        }
//
//        val archiveResourcesTask = project.tasks.create<Zip>("archiveResourceFiles") {
//            group = "build"
////            archiveClassifier.set("resource-files")
//            destinationDirectory.set(project.file("${project.buildDir}/archives"))
//        }
//
//        project.artifacts.add("exportedResourceFiles", archiveResourcesTask)
//
//        project.tasks.create("importResourceFiles") {
//            doLast {
//                importedResourceFilesConf.resolve().forEach {
//                    println(it)
//                }
//            }
//        }
//
//        val component = scf.adhoc("resourceFiles")
//        project.components.add(component)
//        component.addVariantsFromConfiguration(exportedResourceFilesConf) {
//            mapToMavenScope("compile")
//        }
//
//        project.afterEvaluate {
//            archiveResourcesTask.from(ext.dir)
//
//            val publicationResouceFiles = project.extensions.findByType<PublishingExtension>()?.publications?.create<MavenPublication>("resourceFiles") {
//                from(component)
//                this as DefaultMavenPublication
//                isAlias = true
//                artifactId = "$artifactId-resource-files"
//            }
//            project.tasks.withType<AbstractPublishToMaven> {
//                if (publication == publicationResouceFiles) {
//                    doFirst {
//                        val p = publication as DefaultMavenPublication
//                        val projectDependencies = p.apiDependencies.filterIsInstance<DefaultMavenProjectDependency>()
//                        p.apiDependencies.removeAll(projectDependencies)
//                        p.apiDependencies.addAll(projectDependencies.map { TransformedDependency(it) })
//                    }
//                }
//            }
//
//            resourceFilesConf.dependencies.forEach {
//                when (it) {
//                    is ProjectDependency -> importedResourceFilesConf.dependencies.add(it.copy().apply {
//                        targetConfiguration = "exportedResourceFiles"
//                    })
//                    is ExternalModuleDependency -> importedResourceFilesConf.dependencies.add(it.copy().apply {
//                        artifact {
//                            name = "$name-resource-files"
//                        }
//                    })
//                }
//            }
//        }
//    }
//}
