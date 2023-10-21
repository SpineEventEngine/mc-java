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

import com.google.common.collect.ImmutableList
import io.spine.protodata.gradle.CodegenSettings
import io.spine.protodata.gradle.Names.GRADLE_PLUGIN_ID
import io.spine.protodata.gradle.plugin.LaunchProtoData
import io.spine.tools.fs.DirectoryName
import io.spine.tools.gradle.Artifact
import io.spine.tools.mc.annotation.ApiAnnotationsPlugin
import io.spine.tools.mc.java.gradle.generatedGrpcDirName
import io.spine.tools.mc.java.gradle.generatedJavaDirName
import io.spine.tools.mc.java.gradle.mcJavaAnnotation
import io.spine.tools.mc.java.gradle.mcJavaBase
import io.spine.tools.mc.java.gradle.mcJavaRejection
import io.spine.tools.mc.java.gradle.plugins.ProtoDataDecoratorPlugin.Companion.IMPL_CONFIGURATION
import io.spine.tools.mc.java.gradle.plugins.ProtoDataDecoratorPlugin.Companion.PROTODATA_CONFIGURATION
import io.spine.tools.mc.java.gradle.plugins.ProtoDataDecoratorPlugin.Companion.VALIDATION_PLUGIN_CLASS
import io.spine.tools.mc.java.gradle.toolBase
import io.spine.tools.mc.java.gradle.validationJavaBundle
import io.spine.tools.mc.java.gradle.validationJavaRuntime
import io.spine.tools.mc.java.rejection.RejectionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.kotlin.dsl.getByType

/**
 * The plugin that configures ProtoData for the associated project.
 *
 * This plugin does the following:
 *   1. Applies the `io.spine.protodata` plugin.
 *   2. Configures the ProtoData extension of a Gradle project, passing codegen plugins, such
 *      as [JavaValidationPlugin][io.spine.validation.java.JavaValidationPlugin].
 *   3. Creates a [GenerateProtoDataConfig] for passing configuration to ProtoData, and
 *      links it to the [LaunchProtoData] task.
 *   4. Adds required dependencies.
 */
internal class ProtoDataDecoratorPlugin : Plugin<Project> {

    /**
     * Applies the `io.spine.protodata` plugin to the project and, if the user needs
     * validation code generation, configures ProtoData to generate Java validation code.
     *
     * ProtoData configuration is a tricky operation because of Gradle's lifecycle.
     * On one hand, to check if the user disables validation via
     * [skipValidation][io.spine.tools.mc.java.gradle.codegen.ValidationConfig.skipValidation],
     * we need to run configuration after the project is evaluated.
     *
     * At the same time, we need to squeeze our configuration before the `LaunchProtoData` task
     * is configured. This means adding the `afterEvaluate(..)` hook before the ProtoData Gradle
     * plugin is applied to the project.
     */
    override fun apply(project: Project) {
        project.afterEvaluate {
            it.configureProtoData()
        }
        project.pluginManager.apply(GRADLE_PLUGIN_ID)
    }

    companion object {

        // Could be duplicated in auto-generated Gradle code via script plugins in `buildSrc`.
        const val PROTODATA_CONFIGURATION = "protoData"
        const val IMPL_CONFIGURATION = "implementation"

        /**
         * The name of the Validation plugin for ProtoData.
         */
        const val VALIDATION_PLUGIN_CLASS = "io.spine.validation.java.JavaValidationPlugin"
    }
}

private fun Project.configureProtoData() {
    configureProtoDataPlugins()
    tasks.withType(LaunchProtoData::class.java) { launchTask ->
        configureLaunchTask(launchTask)
    }
}

/**
 * Configures ProtoData with plugins, for the given Gradle project.
 */
private fun Project.configureProtoDataPlugins() {
    val codegen = extensions.getByType<CodegenSettings>()
    codegen.plugins(
        VALIDATION_PLUGIN_CLASS,
        RejectionPlugin::class.java.getName(),
        // It should follow `RejectionPlugin` so that rejection throwable types are annotated too.
        ApiAnnotationsPlugin::class.java.getName()
    )
    codegen.subDirs = ImmutableList.of(
        generatedJavaDirName.value(),
        generatedGrpcDirName.value(),
        DirectoryName.kotlin.value()
    )
    addDependencies(
        PROTODATA_CONFIGURATION,
        validationJavaBundle,
        mcJavaBase,
        mcJavaAnnotation,
        mcJavaRejection,
        toolBase
    )
    addDependency(IMPL_CONFIGURATION, validationJavaRuntime)
}

private fun Project.configureLaunchTask(launchTask: LaunchProtoData) {
    val taskName = GenerateProtoDataConfig.taskNameFor(launchTask)
    val configTask = tasks.create(taskName, GenerateProtoDataConfig::class.java) {
        it.apply {
            targetFile.convention(layout.buildDirectory.file(defaultFileName()))
        }
        launchTask.configurationFile.set(it.targetFile)
    }
    // Make the dependency explicit to satisfy Gradle 8.x requirements.
    launchTask.dependsOn(configTask)
}

private fun Project.addDependencies(configurationName: String, vararg artifacts: Artifact) {
    for (artifact in artifacts) {
        addDependency(configurationName, artifact)
    }
}

private fun Project.addDependency(configurationName: String, artifact: Artifact) {
    val dependency = findDependency(artifact)
    dependencies.add(
        configurationName,
        dependency ?: artifact.notation()
    )
}

private fun Project.findDependency(artifact: Artifact): Dependency? {
    val dependencies =
        configurations.stream()
            .flatMap { c ->
                c.dependencies.stream()
            }
    val result = dependencies.filter { d ->
        d.group != null
                && d.group == artifact.group()
                && d.name == artifact.name()
    }.findFirst()
        .orElse(null)
    return result
}

