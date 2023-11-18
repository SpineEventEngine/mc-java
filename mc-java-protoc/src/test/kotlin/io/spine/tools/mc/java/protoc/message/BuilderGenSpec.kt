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

package io.spine.tools.mc.java.protoc.message;

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.Version;
import io.spine.test.tools.mc.java.protoc.BuilderTestProto;
import io.spine.tools.mc.java.codegen.CodegenOptions;
import io.spine.tools.mc.java.protoc.NoOpGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("BuilderGenerator should")
class BuilderGenTest {

    @Test
    @DisplayName("produce builder insertion points")
    void produceBuilderInsertionPoints() {
        var generator = BuilderGen.instance(CodegenOptions.getDefaultInstance());
        var file = BuilderTestProto.getDescriptor();
        var request = CodeGeneratorRequest.newBuilder()
                .addProtoFile(file.toProto())
                .addFileToGenerate(file.getFullName())
                .setCompilerVersion(Version.newBuilder().setMajor(3).build())
                .build();
        var response = generator.process(request);
        var files = response.getFileList();
        assertThat(files).hasSize(1);
        assertThat(files.get(0).getInsertionPoint()).isNotEmpty();
    }

    @Test
    @DisplayName("do nothing if configured to skip validating builders")
    void ignoreIfConfigured() {
        var config = CodegenOptions.newBuilder();
        config.getValidationBuilder().setSkipBuilders(true);
        var generator = BuilderGen.instance(config.build());
        assertThat(generator).isInstanceOf(NoOpGenerator.class);
    }
}
