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

import io.spine.tools.code.SourceSetName
import io.spine.tools.gradle.project.sourceSetNames
import io.spine.tools.gradle.task.TaskWithSourceSetName
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
 *  2.    
 */
public class RoutingPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run {
        applyKspPlugin()
        makeKspDependOnProtoData()
        configureCopyingToGeneratedDir()
        configureSourceSets()
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
        pluginManager.apply(id)
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
        sourceSetNames.forEach { ssn ->
            val kspTaskName = KspTskName(ssn)
            val kspTask = tasks.findByName(kspTaskName.value())
            val protoDataTaskName = ProtoDataTaskName(ssn)
            val protoDataTask = tasks.findByName(protoDataTaskName.value())
            if (protoDataTask != null) {
                kspTask?.dependsOn(protoDataTask)
            }
        }
    }
}

private fun Project.defaultKspTargetDir(ssn: SourceSetName): String =
    "build/generated/ksp/${ssn.value}/kotlin/"

private fun Project.configureCopyingToGeneratedDir() {
    sourceSetNames.forEach { ssn ->

    }
    // From `build/generated/ksp/main/kotlin` etc. to `projectRoot/generated/ksp/`.
}

private fun Project.configureSourceSets() {
    // It should be something like:
    /*
kotlin {
    sourceSets.main {
        kotlin.srcDir("$projectDir/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("$projectDir/generated/ksp/test/kotlin")
    }
    sourceSets.testFixtures {
        kotlin.srcDir("$projectDir/generated/ksp/testFixtures/kotlin")
    }
*/
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
