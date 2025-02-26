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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.dsl.ScriptHandler.CLASSPATH_CONFIGURATION

/**
 * Configures a Gradle project to run KSP with
 * [RouteProcessor][io.spine.tools.mc.java.routing.processor.RouteProcessor].
 */
public class RoutingPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.applyKspPlugin()
    }
}

/**
 * Applies [KspGradlePlugin] if it is not yet added to the project.
 *
 * The function finds the most recent version compatible with the Kotlin
 * runtime of the current Gradle runtime.
 */
private fun Project.applyKspPlugin() =
    with(KspGradlePlugin) {
        if (pluginManager.hasPlugin(id)) {
            return
        }
        val version = findCompatible(KotlinVersion.CURRENT)
        buildscript.dependencies.add(
            CLASSPATH_CONFIGURATION,
            gradlePluginArtifact(version)
        )
        pluginManager.apply(id)
    }

private fun Project.copyToGeneratedDir() {
    // From `build/generated/ksp/main/kotlin` etc. to `projectRoot/generated/ksp/`.
}
