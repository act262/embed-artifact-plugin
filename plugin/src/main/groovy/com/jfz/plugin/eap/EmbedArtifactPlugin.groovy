package com.jfz.plugin.eap

import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.tasks.InvokeManifestMerger
import com.android.build.gradle.tasks.ManifestProcessorTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact

/**
 * process multiple aar/jar embed to one aar/jar
 *
 * @author act262@gmail.com
 */
class EmbedArtifactPlugin implements Plugin<Project> {

    private Project project
    private Configuration embedConf

    def resolvedArtifactSet = new HashSet<ResolvedArtifact>(8)

    @Override
    void apply(Project project) {
        if (!project.pluginManager.hasPlugin("com.android.library")) {
            throw new ProjectConfigurationException("EmbedPlugin must applied in android library project.")
        }

        this.project = project
        createEmbedConfiguration(project)

        project.afterEvaluate {
            resolveArtifact()
            process()
        }
    }

    private void createEmbedConfiguration(Project project) {
        embedConf = project.configurations.create('embed')
        embedConf.visible = false
    }

    private Set<ResolvedArtifact> resolveArtifact() {
        embedConf.resolvedConfiguration.resolvedArtifacts.each { artifact ->
            if ('aar' == artifact.type || 'jar' == artifact.type) {
                println "embed -> [${artifact.type}] => ${artifact.moduleVersion.id}"
                resolvedArtifactSet.add(artifact)
            } else {
                throw new ProjectConfigurationException('Only support embed aar and jar dependencies!', null)
            }
        }
    }

    private void process() {
        for (artifact in resolvedArtifactSet) {
            def file = artifact.file
            def id = artifact.moduleVersion.id

            if ('aar' == artifact.type) {
                // unzip aar
                def root = new File(project.buildDir, "exploded-aar/${id.group}/${id.name}/${id.version}")
                project.copy {
                    from project.zipTree(artifact.file)
                    into root
                    rename('classes.jar', "${file.name}.jar")
                }

                // sourceSet
                project.android.sourceSets.main {
                    aidl.srcDirs "$root/aidl"
                    jniLibs.srcDirs "$root/jni"
                    res.srcDirs "$root/res"
                    assets.srcDirs "$root/assets"
                }

                project.dependencies.add('implementation',
                        project.files("${root.absolutePath}/${file.name}.jar"))
                project.dependencies.add('implementation',
                        project.fileTree("${root.absolutePath}/libs"))

                project.android.libraryVariants.all { LibraryVariant variant ->
                    def variantName = variant.name.capitalize()
                    ManifestProcessorTask processManifestTask = project.tasks["process${variantName}Manifest"]
                    File output = new File(processManifestTask.manifestOutputDirectory, "AndroidManifest.xml")

                    InvokeManifestMerger manifestsMergeTask = project.tasks.create('merge' + variantName + 'Manifest', InvokeManifestMerger)
                    manifestsMergeTask.setVariantName(variantName)
                    manifestsMergeTask.setMainManifestFile(output)
                    manifestsMergeTask.setSecondaryManifestFiles(Collections.singletonList(project.file("$root/AndroidManifest.xml")))
                    manifestsMergeTask.setOutputFile(output)

//                    manifestsMergeTask.dependsOn processManifestTask
                    processManifestTask.finalizedBy manifestsMergeTask
                }

            } else if ('jar' == artifact.type) {
                // copy jar
                def root = new File(project.buildDir, "exploded-jar")

                project.copy {
                    from file
                    into root
                }
                project.dependencies.add('api',
                        project.files("$root/${file.name}"))
            }
        }
    }

}