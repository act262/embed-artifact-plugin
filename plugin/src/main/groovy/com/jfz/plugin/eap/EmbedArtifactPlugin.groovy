package com.jfz.plugin.eap

import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.tasks.InvokeManifestMerger
import com.android.build.gradle.tasks.ManifestProcessorTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
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
            def type = artifact.type

            if ('aar' == type) {
                embedAar(id, file)
            } else if ('jar' == type) {
                embedJar(file)
            }
        }
    }

    private void embedAar(ModuleVersionIdentifier id, File file) {
        // unzip aar into build/exploded-aar/
        def explodedDir = new File(project.buildDir, "exploded-aar/${id.group}/${id.name}/${id.version}")
        project.copy {
            from project.zipTree(file)
            into explodedDir
            rename('classes.jar', "${file.name}.jar")
        }

        // sourceSet
        project.android.sourceSets.main {
            aidl.srcDirs "$explodedDir/aidl"
            jniLibs.srcDirs "$explodedDir/jni"
            res.srcDirs "$explodedDir/res"
            assets.srcDirs "$explodedDir/assets"
        }

        project.dependencies.add('implementation',
                project.files("$explodedDir/${file.name}.jar"))
        project.dependencies.add('implementation',
                project.fileTree("$explodedDir/libs"))

        project.android.libraryVariants.all { LibraryVariant variant ->
            def variantName = variant.name.capitalize()
            ManifestProcessorTask processManifestTask = project.tasks["process${variantName}Manifest"]
            def mainManifestFile = new File(processManifestTask.manifestOutputDirectory, "AndroidManifest.xml")
            def secondaryManifestFiles = Collections.singletonList(project.file("$explodedDir/AndroidManifest.xml"))

            InvokeManifestMerger manifestsMergeTask = project.tasks.create("merge${variantName}Manifest", InvokeManifestMerger)
            manifestsMergeTask.setVariantName(variantName)
            manifestsMergeTask.setMainManifestFile(mainManifestFile)
            manifestsMergeTask.setSecondaryManifestFiles(secondaryManifestFiles)
            manifestsMergeTask.setOutputFile(mainManifestFile)
            manifestsMergeTask.onlyIf {
                secondaryManifestFiles.every {
                    it.exists()
                }
            }
            processManifestTask.finalizedBy manifestsMergeTask
        }
    }

    private void embedJar(File file) {
        // copy jar into build/exploded-jar/
        def explodedDir = new File(project.buildDir, "exploded-jar")

        project.copy {
            from file
            into explodedDir
        }
        project.dependencies.add('api',
                project.files("$explodedDir/${file.name}"))
    }

}