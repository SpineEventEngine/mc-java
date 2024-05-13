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

package io.spine.tools.mc.java.signal

import io.spine.base.MessageFile
import io.spine.protodata.FilePattern
import io.spine.protodata.FilePatternFactory.suffix
import io.spine.protodata.settings.Format
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.testing.PipelineSetup
import io.spine.protodata.testing.PipelineSetup.Companion.byResources
import io.spine.tools.mc.java.gradle.settings.MessageCodegenOptions
import io.spine.tools.mc.java.settings.SignalSettings
import io.spine.type.toJson
import java.nio.file.Path
import org.gradle.testfixtures.ProjectBuilder

/**
 * The abstract base for test suites of the Signal Plugin.
 */
@Suppress("UtilityClassWithPublicConstructor")
internal abstract class SignalPluginTest {

    companion object {

        /**
         * Creates an instance of [SignalSettings] as if it was created by McJava added to
         * a Gradle project.
         */
        fun createSignalSettings(projectDir: Path): SignalSettings {
            val project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
            // This mimics the call `McJavaOptions` perform on `injectProject`.
            val codegenOptions = MessageCodegenOptions(project)
            return codegenOptions.toProto().signalSettings
        }

        /**
         * Creates an instance of [PipelineSetup] with the given parameters.
         *
         * [settings] will be written to the [settingsDir] before creation of
         * a [Pipeline][io.spine.protodata.backend.Pipeline].
         */
        fun setup(outputDir: Path, settingsDir: Path, settings: SignalSettings): PipelineSetup {
            val setup = byResources(
                listOf(SignalPlugin()),
                outputDir,
                settingsDir
            ) {
                writeSettings(it, settings)
            }
            return setup
        }

        private fun writeSettings(settings: SettingsDirectory, signalSettings: SignalSettings) {
            settings.write(
                SignalPlugin.CONSUMER_ID,
                Format.PROTO_JSON,
                signalSettings.toJson()
            )
        }
    }
}

/**
 * Creates [FilePattern] corresponding to this [MessageFile] type.
 */
internal fun MessageFile.pattern(): FilePattern = suffix(suffix())
