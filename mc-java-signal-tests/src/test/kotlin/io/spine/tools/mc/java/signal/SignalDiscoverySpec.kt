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

import com.google.protobuf.Descriptors.FileDescriptor
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.spine.base.EntityState
import io.spine.base.MessageFile
import io.spine.base.MessageFile.COMMANDS
import io.spine.base.MessageFile.EVENTS
import io.spine.base.MessageFile.REJECTIONS
import io.spine.code.proto.FileSet
import io.spine.protodata.File
import io.spine.protodata.file
import io.spine.protodata.matches
import io.spine.protodata.toMessageType
import io.spine.tools.mc.java.WithTypeList
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`SignalDiscovery` should")
internal class SignalDiscoverySpec : SignalPluginTest() {

    @Test
    fun `discover all signals`(
        @TempDir projectDir: Path,
        @TempDir outputDir: Path,
        @TempDir settingsDir: Path
    ) {
        val signalSettings = createSignalSettings(projectDir)
        val setup = setup(outputDir, settingsDir, signalSettings)
        val (pipeline, blackbox) = setup.createPipelineWithBlackBox()

        pipeline()

        /**
         * Asserts that the view state specified by [viewStateClass] contains all
         * the message types declared in this proto file.
         *
         * @param fileType
         *         the type of the file such as `COMMANDS`, `EVENTS`, or `REJECTIONS`.
         * @param viewStateClass
         *         the class of an entity state such as [DiscoveredCommands],
         *         [DiscoveredEvents], or [DiscoveredRejections].
         */
        fun FileDescriptor.assertIfMatches(
            fileType: MessageFile,
            viewStateClass: Class<out EntityState<File>>,
        ) {
            val file = file()
            if (!fileType.pattern().matches(file)) {
                return
            }
            val allMessages = messageTypes.map {
                it.toMessageType()
            }
            val assertEntity = blackbox.assertEntityWithState(file, viewStateClass)
            assertEntity.exists()

            val state = assertEntity.actual()!!.state() as WithTypeList
            state.getTypeList() shouldContainExactlyInAnyOrder allMessages
        }

        // Create a file set with the descriptors for the proto files.
        val fileSet = FileSet.of(setup.request.sourceFileDescriptorsList)

        fileSet.files().forEach {
            it.assertIfMatches(COMMANDS, Commands::class.java)
            it.assertIfMatches(EVENTS, Events::class.java)
            it.assertIfMatches(REJECTIONS, Rejections::class.java)
        }
    }
}
