/*
 * Copyright 2024, TeamDev. All rights reserved.
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

@file:Suppress("TooManyFunctions") // Prefer smaller configuration steps as `fun` over the limit.

package io.spine.tools.mc.java.gradle.plugins

import io.spine.protodata.gradle.CodegenSettings
import io.spine.protodata.gradle.Names
import io.spine.protodata.gradle.Names.GRADLE_PLUGIN_ID
import io.spine.protodata.gradle.plugin.CreateSettingsDirectory
import io.spine.protodata.gradle.plugin.LaunchProtoData
import io.spine.protodata.java.style.JavaCodeStyle
import io.spine.protodata.java.style.JavaCodeStyleFormatterPlugin
import io.spine.protodata.settings.defaultConsumerId
import io.spine.tools.fs.DirectoryName
import io.spine.tools.gradle.Artifact
import io.spine.tools.mc.annotation.ApiAnnotationsPlugin
import io.spine.tools.mc.entity.EntityPlugin
import io.spine.tools.mc.java.gradle.McJava.annotation
import io.spine.tools.mc.java.gradle.McJava.base
import io.spine.tools.mc.java.gradle.McJava.entity
import io.spine.tools.mc.java.gradle.McJava.rejection
import io.spine.tools.mc.java.gradle.ValidationSdk
import io.spine.tools.mc.java.gradle.codegen.MessageCodegenOptions
import io.spine.tools.mc.java.gradle.generatedGrpcDirName
import io.spine.tools.mc.java.gradle.generatedJavaDirName
import io.spine.tools.mc.java.gradle.mcJava
import io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigPlugin.Companion.VALIDATION_PLUGIN_CLASS
import io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigPlugin.Companion.WRITE_PROTODATA_SETTINGS
import io.spine.tools.mc.java.gradle.toolBase
import io.spine.tools.mc.java.rejection.RejectionPlugin
import io.spine.util.theOnly
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import io.spine.protodata.gradle.CodegenSettings as ProtoDataSettings
import io.spine.protodata.plugin.Plugin as ProtoDataPlugin

/**
 * The plugin that configures ProtoData for the associated project.
 *
 * This plugin does the following:
 *   1. Applies the `io.spine.protodata` Gradle plugin to the project.
 *   2. Configures the ProtoData extension of a Gradle project, passing codegen
 *      plugins of ProtoData, such
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
         * The name of the task for writing ProtoData settings.
         */
        const val WRITE_PROTODATA_SETTINGS = "writeProtoDataSettings"

        /**
         * The name of the Validation plugin for ProtoData.
         */
        const val VALIDATION_PLUGIN_CLASS = "io.spine.validation.java.JavaValidationPlugin"

        /**
         * The ID used by Validation plugin components to load the settings.
         */
        const val VALIDATION_SETTINGS_ID = "io.spine.validation.ValidationPlugin"

        /**
         * The ID used by Annotation plugin components to load the settings.
         */
        const val ANNOTATION_SETTINGS_ID = "io.spine.tools.mc.annotation.ApiAnnotationsPlugin"

        /**
         * The ID used by Entity plugin components to load settings.
         */
        const val ENTITY_SETTINGS_ID = "io.spine.tools.mc.entity.EntityPlugin"

        /**
         * The ID for the Java code style settings.
         */
        val JAVA_CODE_STYLE_ID = JavaCodeStyle::class.java.defaultConsumerId
    }
}

private fun Project.configureProtoData() {
    configureProtoDataPlugins()
    val writeSettingsTask = createWriteSettingsTask()
    tasks.withType<LaunchProtoData>().all { task ->
        task.dependsOn(writeSettingsTask)
    }
}

private fun Project.createWriteSettingsTask(): WriteProtoDataSettings {
    val settingsDirTask = tasks.withType(CreateSettingsDirectory::class.java).theOnly()
    val result = tasks.create(WRITE_PROTODATA_SETTINGS, WriteProtoDataSettings::class.java) {
            it.settingsDir.set(settingsDirTask.settingsDir.get())
            it.dependsOn(settingsDirTask)
        }
    return result
}

/**
 * Configures ProtoData with plugins, for the given Gradle project.
 */
private fun Project.configureProtoDataPlugins() {
    // Dependencies common to all the plugins.
    addUserClasspathDependency(
        base,
        toolBase,
    )
    val protodata = extensions.getByType<ProtoDataSettings>()
    setSubdirectories(protodata)

    configureValidation(protodata)
    configureRejections(protodata)
    configureEntities(protodata)

    // Annotations should follow `RejectionPlugin` and `EntityPlugin`
    // so that their output is annotated too.
    configureAnnotations(protodata)

    // The Java style formatting comes last to conclude all the rendering.
    configureStyleFormatting(protodata)
}

private val Project.messageOptions: MessageCodegenOptions
    get() = mcJava.codegen!!

private fun setSubdirectories(protodata: ProtoDataSettings) {
    protodata.subDirs = listOf(
        generatedJavaDirName.value(),
        generatedGrpcDirName.value(),
        DirectoryName.kotlin.value()
    )
}

private fun Project.configureValidation(protodata: ProtoDataSettings) {
    val validationConfig = messageOptions.validation()
    val version = validationConfig.version.get()
    if (validationConfig.enabled.get()) {
        addUserClasspathDependency(ValidationSdk.javaCodegenBundle(version))
        protodata.plugins(
            VALIDATION_PLUGIN_CLASS
        )
    } else {
        addUserClasspathDependency(ValidationSdk.configuration(version))
    }

    // We add the dependency on runtime anyway for the following reasons:
    //  1. We do not want users to change their Gradle build files when they turn on or off
    //     code generation for the validation code.
    //
    //  2. We have run-time validation rules that are going to be used in parallel with
    //     the generated code. This includes current and new implementation for validation
    //     rules for the already existing generated Protobuf code.
    //
    addDependency("implementation", ValidationSdk.javaRuntime(version))
}

private fun Project.configureRejections(protodata: ProtoDataSettings) {
    val rejectionCodegen = messageOptions.rejections()
    if (rejectionCodegen.enabled.get()) {
        addUserClasspathDependency(rejection)
        protodata.addPlugin<RejectionPlugin>()
    }
}

private fun Project.configureEntities(protodata: ProtoDataSettings) {
    addUserClasspathDependency(entity)
    protodata.addPlugin<EntityPlugin>()
}

private fun Project.configureAnnotations(protodata: ProtoDataSettings) {
    addUserClasspathDependency(annotation)
    protodata.addPlugin<ApiAnnotationsPlugin>()
}

private fun configureStyleFormatting(protodata: CodegenSettings) {
    protodata.plugins(
        JavaCodeStyleFormatterPlugin::class.java.canonicalName
    )
}

private fun Project.addUserClasspathDependency(vararg artifacts: Artifact) = artifacts.forEach {
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

private inline fun <reified T: ProtoDataPlugin> ProtoDataSettings.addPlugin() {
    plugins(T::class.java.name)
}
