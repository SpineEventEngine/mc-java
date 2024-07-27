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

package io.spine.tools.mc.java.protoc;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;
import io.spine.tools.mc.java.settings.Combined;
import io.spine.tools.protoc.plugin.EnhancedWithCodeGeneration;
import io.spine.tools.protoc.plugin.TestGeneratorsProto;
import io.spine.type.MessageType;
import io.spine.type.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collection;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.Assertions.assertIllegalArgument;
import static io.spine.testing.Assertions.assertNpe;
import static io.spine.tools.mc.java.protoc.given.CodeGeneratorRequestGiven.protocSettings;
import static io.spine.tools.mc.java.protoc.given.CodeGeneratorRequestGiven.requestBuilder;

@DisplayName("`SpineProtoGenerator` should")
final class CodeGeneratorTest {

    private static final String TEST_PROTO_FILE = "spine/tools/protoc/test_generators.proto";

    private Path pluginSettingsFile;

    @BeforeEach
    void setUp(@TempDir Path tempDirPath) {
        pluginSettingsFile = tempDirPath.resolve("test-spine-mc-java-protoc.pb");
    }

    @DisplayName("concatenate code generated for the same insertion point")
    @Test
    void concatenateGeneratedCode() {
        var settings = Combined.getDefaultInstance();
        var request = requestBuilder()
                .addProtoFile(TestGeneratorsProto.getDescriptor()
                                                 .toProto())
                .addFileToGenerate(TEST_PROTO_FILE)
                .setParameter(protocSettings(settings, pluginSettingsFile))
                .build();
        var type = new MessageType(EnhancedWithCodeGeneration.getDescriptor());
        var firstMethod = "public void test1(){}";
        var secondMethod = "public void test2(){}";
        var firstFile = File.newBuilder()
                .setName("file.proto")
                .setContent(firstMethod)
                .setInsertionPoint(InsertionPoint.class_scope.forType(type))
                .build();
        var secondFile = firstFile.toBuilder()
                .setContent(secondMethod)
                .build();
        ImmutableList<CompilerOutput> compilerOutputs = ImmutableList.of(
                new TestCompilerOutput(firstFile), new TestCompilerOutput(secondFile)
        );
        var generator = new TestGenerator(compilerOutputs);

        var result = generator.process(request);
        assertThat(result.getFileList())
                .hasSize(1);
        var file = result.getFile(0);
        var assertFileContent = assertThat(file.getContent());
        assertFileContent
                .contains(firstMethod);
        assertFileContent
                .contains(secondMethod);
    }

    @DisplayName("drop duplicates in generated code for the same insertion point")
    @Test
    void dropCodeDuplicates() {
        var settings = Combined.getDefaultInstance();
        var request = requestBuilder()
                .addProtoFile(TestGeneratorsProto.getDescriptor()
                                                 .toProto())
                .addFileToGenerate(TEST_PROTO_FILE)
                .setParameter(protocSettings(settings, pluginSettingsFile))
                .build();
        var type = new MessageType(EnhancedWithCodeGeneration.getDescriptor());
        var method = "public void test1(){}";
        var generated = File.newBuilder()
                .setName("file.proto")
                .setContent(method)
                .setInsertionPoint(InsertionPoint.class_scope.forType(type))
                .build();
        ImmutableList<CompilerOutput> compilerOutputs = ImmutableList.of(
                new TestCompilerOutput(generated), new TestCompilerOutput(generated)
        );
        var generator = new TestGenerator(compilerOutputs);

        var result = generator.process(request);
        assertThat(result.getFileList())
                .hasSize(1);
        var file = result.getFile(0);
        var fileContent = assertThat(file.getContent());
        fileContent
                .isEqualTo(method);
    }

    @Nested
    @DisplayName("not process invalid `CodeGeneratorRequest` if passed")
    class Arguments {

        @Test
        @DisplayName("`null`")
        void nullArg() {
            assertNpe(() -> process(null));
        }

        @Test
        @DisplayName("unsupported version")
        void notProcessInvalidRequests() {
            assertIllegalArgument(() -> process(requestWithUnsupportedVersion()));
        }

        @Test
        @DisplayName("empty request")
        void emptyRequest() {
            assertIllegalArgument(() -> process(requestBuilder().build()));
        }

        private void process(CodeGeneratorRequest request) {
            new TestGenerator().process(request);
        }
    }

    private static CodeGeneratorRequest requestWithUnsupportedVersion() {
        var result = requestBuilder();
        result.setCompilerVersion(result.getCompilerVersionBuilder()
                                        .setMajor(2));
        return result.build();

    }

    private static class TestGenerator extends CodeGenerator {

        private final ImmutableList<CompilerOutput> compilerOutputs;

        private TestGenerator() {
            this(ImmutableList.of());
        }

        private TestGenerator(CompilerOutput... outputs) {
            this(ImmutableList.copyOf(outputs));
        }

        private TestGenerator(ImmutableList<CompilerOutput> outputs) {
            compilerOutputs = outputs;
        }

        @Override
        protected Collection<CompilerOutput> generate(Type<?, ?> type) {
            return compilerOutputs;
        }
    }

    private static class TestCompilerOutput extends AbstractCompilerOutput {

        private TestCompilerOutput(File file) {
            super(file);
        }
    }
}
