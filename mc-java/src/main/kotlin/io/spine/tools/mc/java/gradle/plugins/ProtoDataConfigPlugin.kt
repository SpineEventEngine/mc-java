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
import io.spine.protodata.gradle.plugin.LaunchProtoData
import io.spine.tools.fs.DirectoryName
import io.spine.tools.gradle.Artifact
import io.spine.tools.mc.java.gradle.McJava.annotation
import io.spine.tools.mc.java.gradle.McJava.base
import io.spine.tools.mc.java.gradle.McJava.rejection
import io.spine.tools.mc.java.gradle.Validation.javaCodegenBundle
import io.spine.tools.mc.java.gradle.Validation.javaRuntime
import io.spine.tools.mc.java.gradle.generatedGrpcDirName
import io.spine.tools.mc.java.gradle.generatedJavaDirName
import io.spine.tools.mc.java.gradle.mcJava
import io.spine.tools.mc.java.gradle.toolBase
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

/**
 * The plugin that configures ProtoData for the associated project.
 *
 *
 * We use ProtoData and the Validation library to generate validation code right inside
 * the Protobuf message classes. This plugin applies the `io.spine.protodata` plugin,
 * configures its extension, writes the ProtoData configuration file, and adds the required
 * dependencies to the target project.
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
        project.pluginManager.apply(PROTO_DATA_ID)
    }

    companion object {

        /**
         * The ID of the ProtoData Gradle Plugin.
         */
        private const val PROTO_DATA_ID = "io.spine.protodata"

        /**
         * The name of the directory where the ProtoData configuration files are stored.
         */
        private const val CONFIG_SUBDIR = "protodata-config"

        /**
         * The name of the `protoData` configuration.
         */
        private const val PROTODATA_CONFIGURATION = "protoData"

        /**
         * The name of the `implementation` configuration.
         */
        private const val IMPL_CONFIGURATION = "implementation"

        fun configTaskName(launchTask: String): String = "writeConfigFor_${launchTask}"
    }

    private fun Project.configureProtoData() {
        configurePlugins()
        tasks.withType(LaunchProtoData::class.java) { task ->
            val taskName = configTaskName(task.name)
            val configTask = tasks.create(taskName, GenerateProtoDataConfig::class.java) {
                t -> linkConfigFile(task, t)
            }
            task.dependsOn(configTask)
        }
    }

    /**
     * Configures ProtoData with plugins, for the given Gradle project.
     */
    private fun Project.configurePlugins() {
        val codegen = extensions.getByType<CodegenSettings>()
        configureValidationRendering(codegen)

        codegen.plugins(
            "io.spine.tools.mc.java.rejection.RejectionPlugin"
        )

        codegen.subDirs = listOf(
            generatedJavaDirName.value(),
            generatedGrpcDirName.value(),
            DirectoryName.kotlin.value()
        )

        addDependencies(
            PROTODATA_CONFIGURATION,
            base,
            annotation,
            rejection,
            toolBase
        )
    }

    private fun Project.configureValidationRendering(codegen: CodegenSettings) {
        codegen.plugins(
            "io.spine.validation.java.JavaValidationPlugin"
        )
        val version = mcJava.codegen.validation().version.get()
        addDependency(PROTODATA_CONFIGURATION, javaCodegenBundle(version))
        addDependency(IMPL_CONFIGURATION, javaRuntime(version))
    }

    private fun Project.linkConfigFile(launch: LaunchProtoData, config: GenerateProtoDataConfig) {
        val targetFile = config.targetFile
        val fileName = config.name + ".bin"
        val defaultFile = layout.buildDirectory.file(CONFIG_SUBDIR + File.separatorChar + fileName)
        targetFile.convention(defaultFile)
        launch.configurationFile.set(targetFile)
    }
}

private fun Project.addDependency(configurationName: String, artifact: Artifact) {
    val dependency = findDependency(artifact) ?: artifact.notation()
    dependencies.add(configurationName, dependency)
}

private fun Project.addDependencies(configurationName: String, vararg artifacts: Artifact) {
    for (artifact in artifacts) {
        addDependency(configurationName, artifact)
    }
}

private fun Project.findDependency(artifact: Artifact): Dependency? {
    val dependencies = configurations.flatMap { c -> c.dependencies }
    val found = dependencies.firstOrNull { d ->
        d.group?.let { it == artifact.group() && d.name == artifact.name() }
            ?: false
    }
    return found
}
