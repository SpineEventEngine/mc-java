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

package io.spine.tools.mc.java.signal

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.base.RejectionMessage
import io.spine.string.Indent.Companion.defaultJavaIndent
import io.spine.string.count
import io.spine.string.repeat
import io.spine.tools.java.reference
import java.nio.file.Path
import kotlin.io.path.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`RejectionRenderer` should")
internal class RejectionRendererSpec : SignalPluginTest() {

    companion object {

        /**
         * The number of rejections declared in `given/signals/rejections.proto`.
         */
        const val DECLARED_REJECTIONS = 6

        lateinit var rejectionCode: String

        @BeforeAll
        @JvmStatic
        fun setup(
            @TempDir projectDir: Path,
            @TempDir outputDir: Path,
            @TempDir settingsDir: Path
        ) {
            val sourceFileSet = runPipeline(projectDir, outputDir, settingsDir)
            val sourceFile = sourceFileSet.find(
                Path("io/spine/tools/mc/signal/given/rejection/Rejections.java")
            )
            sourceFile shouldNotBe null
            rejectionCode = sourceFile!!.code()
        }
    }

    @Test
    @Disabled("Until migration to interface generation based on ProtoData")
    fun `add 'RejectionMessage' interface`() {
        rejectionCode.count(
            "${RejectionMessage::class.java.reference},"
        ) shouldBe DECLARED_REJECTIONS
    }

    @Test
    fun `render the 'Field' class for each rejection message`() {
        // Ensure that the `Field` classes are nested under the corresponding
        // rejection message types.
        val decl = defaultJavaIndent.repeat(2) + FIELD_CLASS_SIGNATURE
        rejectionCode.count(decl) shouldBe DECLARED_REJECTIONS
    }
}
