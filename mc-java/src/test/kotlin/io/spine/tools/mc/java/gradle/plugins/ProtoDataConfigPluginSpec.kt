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

import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldNotBe
import io.spine.protodata.gradle.CodegenSettings
import io.spine.protodata.gradle.plugin.Extension
import io.spine.tools.mc.annotation.ApiAnnotationsPlugin
import io.spine.tools.mc.java.gradle.given.StubProject
import io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigPlugin.Companion.VALIDATION_PLUGIN_CLASS
import io.spine.tools.mc.java.rejection.RejectionPlugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.kotlin.dsl.findByType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ProtoDataConfigPlugin` should")
internal class ProtoDataConfigPluginSpec {

    companion object {

        lateinit var project: Project

        @BeforeAll
        @JvmStatic
        fun createProjectAndPlugins() {
            project = StubProject.createFor(this::class.java)
                .withMavenRepositories()
                .get()

            val plugins = project.pluginManager
            plugins.apply("java")
            plugins.apply("com.google.protobuf")
            plugins.apply(ProtoDataConfigPlugin::class.java)

            // Evaluate the project so that `project.afterEvaluate` is called.
            project.getTasksByName("fooBar", false)
        }
    }

    @Test
    fun `add ProtoData plugins`() {
        val codegenSettings = project.extensions.findByType<CodegenSettings>() as Extension

        codegenSettings shouldNotBe null

        // Obtain a value of the `plugins` property via reflection.
        val getter = Extension::class.java.getDeclaredMethod("getPlugins\$gradle_plugin")
        getter.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val propValue = getter.invoke(codegenSettings) as ListProperty<String>
        val plugins = propValue.get()

        plugins.shouldContainInOrder(
            VALIDATION_PLUGIN_CLASS,
            RejectionPlugin::class.java.name,
            ApiAnnotationsPlugin::class.java.name
        )
    }
}
