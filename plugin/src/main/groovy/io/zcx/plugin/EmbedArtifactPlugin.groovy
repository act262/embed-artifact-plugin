package io.zcx.plugin

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.internal.dsl.BuildType
import com.android.build.gradle.tasks.InvokeManifestMerger
import com.android.build.gradle.tasks.ManifestProcessorTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact

/**
 * Process multiple artifact(aar、jar) merge into one artifact.
 *
 * @author act262@gmail.com
 */
class EmbedArtifactPlugin implements Plugin<Project> {

    private Project project

    def resolvedAar = new HashSet<ResolvedArtifact>(4)
    def resolvedJar = new HashSet<ResolvedArtifact>(4)

    @Override
    void apply(Project project) {
        if (!project.pluginManager.hasPlugin("com.android.library")) {
            throw new ProjectConfigurationException("EmbedArtifactPlugin must applied in android library project.")
        }

        this.project = project
        createEmbedConfiguration()

        project.afterEvaluate {
            resolveArtifact()
            process()
        }
    }

    private void createEmbedConfiguration() {
        def embedConf = project.configurations.create('embed')
        embedConf.visible = false
    }

    private Set<ResolvedArtifact> resolveArtifact() {
        project.configurations.embed.resolvedConfiguration.resolvedArtifacts.each { artifact ->
            if ('aar' == artifact.type) {
                resolvedAar.add(artifact)
            } else if ('jar' == artifact.type) {
                resolvedJar.add(artifact)
            } else {
                throw new ProjectConfigurationException('Only support embed aar and jar dependencies!', null)
            }
        }
    }

    private void process() {
        for (artifact in resolvedAar) {
            processAar(artifact)
        }

        for (artifact in resolvedJar) {
            processJar(artifact)
        }
    }

    private void processAar(ResolvedArtifact artifact) {
        embedAar(artifact.moduleVersion.id, artifact.file)
    }

    private void processJar(ResolvedArtifact artifact) {
        embedJar(artifact.file)
    }

    private void embedAar(ModuleVersionIdentifier id, File file) {
        // unzip aar into build/exploded-aar/
        def explodedDir = new File(project.buildDir, "exploded-aar/${id.group}/${id.name}/${id.version}")
        // rename xxx.aar to xxx.jar
        def newName = "${file.name.replace(".aar", ".jar")}"

        project.copy {
            from project.zipTree(file)
            into explodedDir
            rename('classes.jar', newName)
        }

        // sourceSet
        project.android.sourceSets.main {
            aidl.srcDirs "$explodedDir/aidl"
            jniLibs.srcDirs "$explodedDir/jni"
            res.srcDirs "$explodedDir/res"
            assets.srcDirs "$explodedDir/assets"
        }

        project.dependencies.add('implementation',
                project.files("$explodedDir/$newName"))
        project.dependencies.add('implementation',
                project.fileTree("$explodedDir/libs"))

        project.android.libraryVariants.all { LibraryVariant variant ->
            def variantName = variant.name.capitalize()
            ManifestProcessorTask processManifestTask = project.tasks["process${variantName}Manifest"]
            def mainManifestFile = new File(processManifestTask.manifestOutputDirectory, "AndroidManifest.xml")
            def secondaryManifestFiles = Collections.singletonList(project.file("$explodedDir/AndroidManifest.xml"))

            InvokeManifestMerger manifestsMergeTask = project.tasks.create("merge${id.name.capitalize()}${variantName}Manifest", InvokeManifestMerger)
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

        project.android.buildTypes.each { BuildType buildType ->
            // contains library proguard.txt
            buildType.consumerProguardFiles("$explodedDir/proguard.txt")
            buildType.proguardFiles("$explodedDir/proguard.txt")
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