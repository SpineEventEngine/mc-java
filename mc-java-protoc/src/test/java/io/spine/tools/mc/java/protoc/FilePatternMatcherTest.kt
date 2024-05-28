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
package io.spine.tools.mc.java.protoc

import io.kotest.matchers.shouldBe
import io.spine.protodata.FilePattern
import io.spine.protodata.FilePatternFactory.prefix
import io.spine.protodata.FilePatternFactory.regex
import io.spine.protodata.FilePatternFactory.suffix
import io.spine.testing.Assertions.assertNpe
import io.spine.tools.protoc.plugin.FPMMessage
import io.spine.type.MessageType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`FilePatternMatcher` should")
internal class FilePatternMatcherTest {

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // Force passing `null`s.
    @Nested inner class
    `throw 'NullPointerException' if` {

        @Test
        fun `null pattern passed`() {
            assertNpe {
                FilePatternMatcher(null)
            }
        }

        @Test
        fun `null 'MessageType' supplied`() {
            assertNpe {
                val pattern = FilePattern.getDefaultInstance()
                FilePatternMatcher(pattern).test(null)
            }
        }
    }

    @Nested inner class
    Match {

        @Test
        fun `suffix pattern`() {
            assertMatches(suffix("file_patterns.proto"))
        }

        @Test
        fun `prefix pattern`() {
            assertMatches(prefix("spine/tools/protoc/test_file"))
        }

        @Test
        fun `regex pattern`() {
            assertMatches(regex(".*tools\\/protoc\\/.*file_patterns.*"))
        }

        private fun assertMatches(pattern: FilePattern) {
            val matcher = FilePatternMatcher(pattern)
            val type = MessageType(FPMMessage.getDescriptor())
            matcher.test(type) shouldBe true
        }
    }

    @Nested inner class
    `Not match` {

        @Test
        fun `suffix pattern`() {
            assertNotMatches(suffix("test_file.proto"))
        }

        @Test
        fun `prefix pattern`() {
            assertNotMatches(prefix("spine/tools/protoc/test_patterns"))
        }

        @Test
        fun `regex pattern`() {
            assertNotMatches(regex(".*tools\\/protoc\\/.*test_patterns.*"))
        }

        private fun assertNotMatches(pattern: FilePattern) {
            val matcher = FilePatternMatcher(pattern)
            val type = MessageType(FPMMessage.getDescriptor())
            matcher.test(type) shouldBe false
        }
    }
}
