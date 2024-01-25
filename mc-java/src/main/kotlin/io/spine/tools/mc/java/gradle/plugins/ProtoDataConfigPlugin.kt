/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.spine.tools.mc.java.gradle.plugins

import io.spine.protodata.gradle.CodegenSettings
import io.spine.protodata.gradle.Names
import io.spine.protodata.gradle.Names.GRADLE_PLUGIN_ID
import io.spine.protodata.gradle.plugin.CreateSettingsDirectory
import io.spine.protodata.gradle.plugin.LaunchProtoData
import io.spine.tools.fs.DirectoryName
import io.spine.tools.gradle.Artifact
import io.spine.tools.mc.annotation.ApiAnnotationsPlugin
import io.spine.tools.mc.java.gradle.McJava.annotation
import io.spine.tools.mc.java.gradle.McJava.base
import io.spine.tools.mc.java.gradle.McJava.rejection
import io.spine.tools.mc.java.gradle.Validation.javaCodegenBundle
import io.spine.tools.mc.java.gradle.Validation.javaRuntime
import io.spine.tools.mc.java.gradle.generatedGrpcDirName
import io.spine.tools.mc.java.gradle.generatedJavaDirName
import io.spine.tools.mc.java.gradle.mcJava
import io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigPlugin.Companion.CONFIG_SUBDIR
import io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigPlugin.Companion.VALIDATION_PLUGIN_CLASS
import io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigPlugin.Companion.configTaskName
import io.spine.tools.mc.java.gradle.toolBase
import io.spine.tools.mc.java.rejection.RejectionPlugin
import io.spine.util.theOnly
import java.io.File.separatorChar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

/**
 * The plugin that configures ProtoData for the associated project.
 *
 * This plugin does the following:
 *   1. Applies the `io.spine.protodata` plugin.
 *   2. Configures the ProtoData extension of a Gradle project, passing codegen plugins, such
 *      as [JavaValidationPlugin][io.spine.validation.java.JavaValidationPlugin].
 *   3. Creates a [WriteProtoDataSettings] task for passing configuration to ProtoData, and
 *      links it to the [LaunchProtoData] task.
 *   4. Adds required dependencies.
 */
internal class ProtoDataConfigPlugin : Plugin<Project> {

    /**
     * Applies the `io.spine.protodata` plugin to the project and, if the user needs
     * validation code generation, configures ProtoData to generate Java validation code.
     *
     * ProtoData configuration is a tricky operation because of Gradle's lifecycle.
     * We need to squeeze our configuration before the `LaunchProtoData` task is configured.
     * This means adding the `afterEvaluate(..)` hook before the ProtoData Gradle plugin
     * is applied to the project.
     */
    override fun apply(project: Project) {
        project.afterEvaluate {
            it.configureProtoData()
        }
        project.pluginManager.apply(GRADLE_PLUGIN_ID)
    }

    companion object {

        /**
         * The name of the directory where the ProtoData configuration files are stored.
         */
        const val CONFIG_SUBDIR = "protodata-config"

        /**
         * The name of the Validation plugin for ProtoData.
         */
        const val VALIDATION_PLUGIN_CLASS = "io.spine.validation.java.JavaValidationPlugin"

        /**
         * Obtains the task name for writing the ProtoData configuration file for the given
         * name of the `LaunchProtoData` task.
         */
        fun configTaskName(launchTask: String): String = "writeConfigFor_${launchTask}"
    }

    private fun Project.configureProtoData() {
        configurePlugins()
        tasks.withType<LaunchProtoData>().all { task ->
            task.createConfigTask()
        }
    }
}

/**
 * Configures ProtoData with plugins, for the given Gradle project.
 */
private fun Project.configurePlugins() {
    val protodata = extensions.getByType<CodegenSettings>()
    configureValidationRendering(protodata)
    configureRejectionRendering(protodata)

    protodata.plugins(
        // It should follow `RejectionPlugin` so that rejection throwable types are annotated too.
        ApiAnnotationsPlugin::class.java.getName()
    )

    setSubdirectories(protodata)
    addUserClasspathDependencies(
        base,
        toolBase,
        annotation,
    )
}

private fun Project.configureValidationRendering(protodata: CodegenSettings) {
    val validationConfig = mcJava.codegen.validation()
    if (validationConfig.enabled.get()) {
        protodata.plugins(
            VALIDATION_PLUGIN_CLASS
        )
        val version = validationConfig.version.get()
        addDependency(Names.USER_CLASSPATH_CONFIGURATION, javaCodegenBundle(version))
        addDependency("implementation", javaRuntime(version))
    }
}

private fun Project.configureRejectionRendering(protodata: CodegenSettings) {
    val rejectionCodegen = mcJava.codegen.rejections()
    if (rejectionCodegen.enabled.get()) {
        protodata.plugins(
            RejectionPlugin::class.java.getName()
        )
        addUserClasspathDependencies(rejection)
    }
}

private fun setSubdirectories(protodata: CodegenSettings) {
    protodata.subDirs = listOf(
        generatedJavaDirName.value(),
        generatedGrpcDirName.value(),
        DirectoryName.kotlin.value()
    )
}

private fun LaunchProtoData.createConfigTask() {
    val taskName = configTaskName(name)
    val settingsDirTask = project.tasks.withType(CreateSettingsDirectory::class.java).theOnly()
    val configTask = project.tasks.create(taskName, WriteProtoDataSettings::class.java) { t ->
        t.settingsDir.set(settingsDirTask.settingsDir.get())
        t.dependsOn(settingsDirTask)
        // linkConfigFile(t)
    }
    dependsOn(configTask)
}

//private fun LaunchProtoData.linkConfigFile(config: WriteProtoDataSettings) {
//    val targetFile = config.file()
//    settingsDir.set(config.settingsDir.get())
//}

/**
 * Configures the `targetFile` property of this task with the conventional path and returns it.
 */
//private fun WriteProtoDataSettings.file(): Provider<RegularFile> {
//    val fileName = "$name.bin"
//    val defaultFile = project.layout.buildDirectory.file(CONFIG_SUBDIR + separatorChar + fileName)
//    targetFile.convention(defaultFile)
//    return targetFile
//}

private fun Project.addUserClasspathDependencies(vararg artifacts: Artifact) = artifacts.forEach {
    addDependency(Names.USER_CLASSPATH_CONFIGURATION, it)
}

private fun Project.addDependency(configuration: String, artifact: Artifact) {
    val dependency = findDependency(artifact) ?: artifact.notation()
    dependencies.add(configuration, dependency)
}

private fun Project.findDependency(artifact: Artifact): Dependency? {
    val dependencies = configurations.flatMap { c -> c.dependencies }
    val found = dependencies.firstOrNull { d ->
        artifact.group() == d.group // `d.group` could be `null`.
                && artifact.name() == d.name
    }
    return found
}
