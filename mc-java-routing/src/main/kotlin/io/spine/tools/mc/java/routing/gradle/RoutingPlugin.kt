/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.tools.mc.java.routing.gradle

import com.google.devtools.ksp.gradle.KspTaskJvm
import io.spine.tools.code.SourceSetName
import io.spine.tools.gradle.project.sourceSetNames
import io.spine.tools.gradle.protobuf.generatedDir
import io.spine.tools.gradle.task.TaskWithSourceSetName
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.dsl.ScriptHandler.CLASSPATH_CONFIGURATION

/**
 * Configures a Gradle project to run [KSP](https://kotlinlang.org/docs/ksp-overview.html) with
 * [RouteProcessor][io.spine.tools.mc.java.routing.processor.RouteProcessor].
 *
 * The plugin performs the following configuration steps:
 *
 *  1. Adds the [KSP Gradle Plugin](https://github.com/google/ksp) if it is not added already.
 *     When adding, we find the most recent version of the KSP Gradle Plugin which
 *     is compatible with the version of Kotlin used in the current Gradle runtime.
 *
 *  2. Makes a KSP task depend on a `LaunchProtoData` task for the same source set.
 *
 *  3. Replace the output of KSP tasks to generate the code to [Project.generatedDir]
 *     instead of the default directory set by the KSP Gradle Plugin.
 */
public class RoutingPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run {
        applyKspPlugin()
        makeKspDependOnProtoData()
        replaceKspOutputDirs()
    }
}

/**
 * Applies [KspGradlePlugin], if it is not yet added, to this project.
 */
private fun Project.applyKspPlugin() =
    with(KspGradlePlugin) {
        if (pluginManager.hasPlugin(id)) {
            return
        }
        val alreadyInClasspath =
            rootProject.buildscriptClasspathHas(module)
                    || project.buildscriptClasspathHas(module)
        if (!alreadyInClasspath) {
            val version = findCompatible(KotlinVersion.CURRENT)
            buildscript.dependencies.add(
                CLASSPATH_CONFIGURATION,
                gradlePluginArtifact(version)
            )
        }
        project.afterEvaluate {
            pluginManager.apply(id)
        }
    }

/**
 * Makes `ksp<SourceSetName>Kotlin` tasks depend on corresponding
 * `launch<SourceSetName>ProtoData` tasks.
 *
 * This dependency is needed to avoid Gradle warning on disabled execution
 * optimization because of the absence of explicit or implicit dependencies.
 */
private fun Project.makeKspDependOnProtoData() {
    afterEvaluate {
        val kspTasks = kspTasks()
        kspTasks.forEach { (ssn, kspTask) ->
            val protoDataTaskName = ProtoDataTaskName(ssn)
            val protoDataTask = tasks.findByName(protoDataTaskName.value())
            if (protoDataTask != null) {
                kspTask.dependsOn(protoDataTask)
            }
        }
    }
}

/**
 * Lists KSP tasks found in this project for its source sets.
 */
private fun Project.kspTasks(): Map<SourceSetName, KspTaskJvm> {
    val withNulls = sourceSetNames.associateWith { ssn ->
        val kspTaskName = KspTskName(ssn)
        tasks.findByName(kspTaskName.value())
    }
    val result = withNulls.filterValues { it != null }
        .mapValues { it.value!! as KspTaskJvm }
    return result
}

/**
 * The function replaces default destination directory defied by
 * [com.google.devtools.ksp.gradle.KspGradleSubplugin.getKspOutputDir] to
 * the one we used for all the generated code at the level of the project root.
 */
private fun Project.replaceKspOutputDirs() {
    afterEvaluate {
        kspTasks().forEach { (_, kspTask) ->
            val current = kspTask.destination.get().path
            val replaced = current.replace("$buildDir/generated", generatedDir.toString())
            kspTask.destination.set(File(replaced))
        }
    }
}

//TODO:2025-02-27:alexander.yevsyukov: This should go to ToolBase.
public class KspTskName(ssn: SourceSetName) :
    TaskWithSourceSetName("ksp${ssn.toInfix()}Kotlin", ssn)

//TODO:2025-02-27:alexander.yevsyukov: This should be a part of ProtoData.
/**
 * The name of the `LaunchProtoData` task for the given source set.
 */
public class ProtoDataTaskName(ssn: SourceSetName) :
    TaskWithSourceSetName("launch${ssn.toInfix()}ProtoData", ssn)

/**
 * Tells if the given module already present in the `compile` configuration
 * of the `buildscript` of this project.
 */
private fun Project.buildscriptClasspathHas(module: String): Boolean {
    val classpath = buildscript.configurations.findByName(CLASSPATH_CONFIGURATION)
    return classpath?.let {
        it.dependencies.any { dep -> "${dep.group}:${dep.name}" == module }
    } ?: false
}
