/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java

import com.google.protobuf.Message
import io.spine.protodata.java.style.JavaCodeStyleFormatterPlugin
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.settings.Format
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.testing.PipelineSetup
import io.spine.protodata.testing.PipelineSetup.Companion.byResources
import io.spine.tools.mc.java.gradle.settings.CodegenConfig
import io.spine.type.toJson
import java.nio.file.Path
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * An abstract base for companion objects for the suites testing ProtoData plugins
 * provided by other modules.
 */
abstract class PluginTestSetup<S: Message>(
    protected val plugin: Plugin,
    protected val settingsId: String
) {

    /**
     * Creates a Gradle project to be used in the tests.
     *
     * @param dir the project directory.
     */
    protected fun createProject(dir: Path): Project {
        return ProjectBuilder.builder().withProjectDir(dir.toFile()).build()
    }

    /**
     * Creates settings for the [plugin] under the test.
     *
     * Please use [createCodegenConfig] for obtaining all default code
     * generation settings and then get its part for your plugin.
     */
    protected abstract fun createSettings(projectDir: Path): S

    /**
     * Creates default code generation settings created for a project when
     * McJava Gradle plugin is applied.
     */
    protected fun createCodegenConfig(projectDir: Path): CodegenConfig {
        val project = createProject(projectDir)
        // This mimics the call `McJavaOptions` perform on `injectProject`.
        val codegenConfig = CodegenConfig(project)
        return codegenConfig
    }

    /**
     * Creates an instance of [PipelineSetup] with the given parameters.
     *
     * [settings] will be written to the [settingsDir] before creation of
     * a [Pipeline][io.spine.protodata.backend.Pipeline].
     */
    fun setup(outputDir: Path, settingsDir: Path, settings: S): PipelineSetup {
        val setup = byResources(
            listOf(
                plugin,
                // We want to be able to see the code in debug with human eyes. Mercy!..
                JavaCodeStyleFormatterPlugin()
            ),
            outputDir,
            settingsDir
        ) {
            writeSettings(it, settings)
        }
        return setup
    }

    private fun writeSettings(dir: SettingsDirectory, settings: S) {
        dir.write(settingsId, Format.PROTO_JSON, settings.toJson())
    }

    /**
     * Runs the pipeline with the default plugin settings.
     *
     * For running a pipeline with custom settings, please call [createSettings], modify
     * the returned instance and then:
     *
     * ```kotlin
     * val setup = setup(outputDir, settingsDir, settings)
     * val pipeline = setup.createPipeline()
     * pipeline()
     * ```
     *
     * @see createSettings
     */
    fun runWithDefaultSettings(
        projectDir: Path,
        outputDir: Path,
        settingsDir: Path
    ): SourceFileSet {
        // Clear the cache of previously parsed files to avoid repeated code generation.
        SourceFile.clearCache()
        val settings = createSettings(projectDir)
        val setup = setup(outputDir, settingsDir, settings)
        val pipeline = setup.createPipeline()
        pipeline()
        return setup.sourceFileSet
    }
}
