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

import io.spine.base.CommandMessage
import io.spine.base.EventMessage
import io.spine.base.MessageFile
import io.spine.base.MessageFile.COMMANDS
import io.spine.base.MessageFile.EVENTS
import io.spine.base.MessageFile.REJECTIONS
import io.spine.base.RejectionMessage
import io.spine.protodata.FilePattern
import io.spine.protodata.FilePatternFactory.suffix
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.settings.Format
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.testing.PipelineSetup
import io.spine.tools.java.code.javaClassName
import io.spine.tools.mc.java.settings.addInterface
import io.spine.tools.mc.java.settings.signalSettings
import io.spine.tools.mc.java.settings.signals
import java.nio.file.Path

@Suppress("UtilityClassWithPublicConstructor")
internal abstract class SignalPluginTest {

    companion object {

        fun createPipeline(settingsDir: Path, outputDir: Path): Pipeline {
            val setup = PipelineSetup.byResources(
                listOf(SignalPlugin()),
                outputDir,
                settingsDir
            ) { settings -> writeSettings(settings) }

            return setup.createPipeline()
        }

        private fun writeSettings(settings: SettingsDirectory) {
            val signalSettings = signalSettings {
                commands = signals {
                    pattern.add(COMMANDS.pattern())
                    addInterface.add(CommandMessage::class.java.asInterface())
                }
                events = signals {
                    pattern.add(EVENTS.pattern())
                    addInterface.add(EventMessage::class.java.asInterface())
                    //TODO:2024-04-19:alexander.yevsyukov: Add generate fields.
                }
                rejections = signals {
                    pattern.add(REJECTIONS.pattern())
                    addInterface.add(RejectionMessage::class.java.asInterface())
                    //TODO:2024-04-19:alexander.yevsyukov: Add generate fields.
                }
            }
            settings.write(
                SignalPlugin.CONSUMER_ID,
                Format.PROTO_JSON,
                signalSettings.toByteArray()
            )
        }
    }
}

private fun <T> Class<T>.asInterface() = addInterface {
    name = javaClassName {
        canonical = canonicalName
    }
}

private fun MessageFile.pattern(): FilePattern = suffix(suffix())
