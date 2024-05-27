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

package io.spine.tools.mc.java.mgroup

import io.kotest.matchers.string.shouldContain
import io.spine.protodata.java.style.JavaCodeStyleFormatterPlugin
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.settings.Format
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.testing.PipelineSetup
import io.spine.protodata.testing.PipelineSetup.Companion.byResources
import io.spine.tools.mc.java.gradle.settings.CodegenConfig
import io.spine.tools.mc.java.mgroup.given.CustomField
import io.spine.tools.mc.java.settings.GroupSettings
import io.spine.type.toJson
import java.nio.file.Path
import kotlin.io.path.Path
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`GroupedMessageRenderer` should")
internal class GroupedMessageRendererSpec {

    companion object {

        lateinit var sourceFileSet: SourceFileSet
        lateinit var code: String

        @BeforeAll
        @JvmStatic
        fun runPipeline(
            @TempDir projectDir: Path,
            @TempDir outputDir: Path,
            @TempDir settingsDir: Path
        ) {
            val settings = createSettings(projectDir)
            val setup = setup(outputDir, settingsDir, settings)
            val pipeline = setup.createPipeline()
            pipeline()
            sourceFileSet = setup.sourceFileSet
            code = sourceFileSet.find(
                Path("io/spine/tools/mc/mgroup/given/Student.java")
            )!!.code()
        }

        private fun setup(
            outputDir: Path,
            settingsDir: Path,
            settings: GroupSettings
        ): PipelineSetup {
            val setup = byResources(
                listOf(
                    MessageGroupPlugin(),
                    JavaCodeStyleFormatterPlugin()
                ),
                outputDir,
                settingsDir
            ) {
                writeSettings(it, settings)
            }
            return setup
        }

        private fun createSettings(projectDir: Path): GroupSettings {
            val project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
            val codegenOptions = CodegenConfig(project)
            codegenOptions.forMessage("given.signals.Student") {
                it.markFieldsAs(CustomField::class.java.canonicalName)
            }
            return codegenOptions.toProto().groupSettings
        }

        private fun writeSettings(settings:SettingsDirectory, groupSettings: GroupSettings) {
            settings.write(
                MessageGroupPlugin.SETTINGS_ID,
                Format.PROTO_JSON,
                groupSettings.toJson()
            )
        }
    }

    @Test
    fun `render 'Field' class`() {
        code shouldContain "public static final class Field"
    }
}