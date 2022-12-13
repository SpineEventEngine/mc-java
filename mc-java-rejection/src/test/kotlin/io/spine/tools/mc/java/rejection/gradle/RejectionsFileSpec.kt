/*
 * Copyright 2022, TeamDev. All rights reserved.
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
package io.spine.tools.mc.java.rejection.gradle

import com.google.common.testing.NullPointerTester
import com.google.common.truth.Truth
import com.google.protobuf.EmptyProto
import io.spine.test.code.proto.FakeRejectionsProto
import io.spine.test.code.proto.MoreFakeRejections
import io.spine.testing.DisplayNames
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`RejectionsFile` should")
internal class RejectionsFileSpec {
    @Test
    @DisplayName(DisplayNames.NOT_ACCEPT_NULLS)
    fun nonNull() {
        NullPointerTester()
            .testAllPublicStaticMethods(RejectionsFile::class.java)
    }

    @Test
    @DisplayName("accept only files ending with `rejections.proto`")
    fun checkFileName() {
        val sourceFile = SourceFile.from(EmptyProto.getDescriptor())
        val exception = Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { RejectionsFile.from(sourceFile) }
        Truth.assertThat(exception)
            .hasMessageThat()
            .contains("`rejections.proto`")
    }

    @Test
    @DisplayName("accept only files with `Rejections` outer class name")
    fun checkOuterClassName() {
        val sourceFile = SourceFile.from(FakeRejectionsProto.getDescriptor())
        val exception = Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { RejectionsFile.from(sourceFile) }
        Truth.assertThat(exception)
            .hasMessageThat()
            .contains("`Rejections`")
    }

    @Test
    @DisplayName("accept only files with `java_multiple_files = false`")
    fun checkMultipleFiles() {
        val sourceFile = SourceFile.from(MoreFakeRejections.getDescriptor())
        val exception = Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { RejectionsFile.from(sourceFile) }
        Truth.assertThat(exception)
            .hasMessageThat()
            .contains("`java_multiple_files`")
    }
}
