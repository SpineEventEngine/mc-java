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

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;
import io.spine.option.OptionsProvider;
import io.spine.protodata.FilePatternFactory;
import io.spine.tools.java.fs.SourceFile;
import io.spine.tools.mc.java.protoc.given.TestInterface;
import io.spine.tools.mc.java.protoc.given.TestMethodFactory;
import io.spine.tools.mc.java.protoc.given.TestNestedClassFactory;
import io.spine.tools.mc.java.protoc.given.UuidMethodFactory;
import io.spine.tools.mc.java.settings.CodegenSettings;
import io.spine.tools.mc.java.settings.GroupSettings;
import io.spine.tools.mc.java.settings.MessageGroup;
import io.spine.tools.mc.java.settings.Uuids;
import io.spine.tools.protoc.plugin.EnhancedWithCodeGeneration;
import io.spine.tools.protoc.plugin.TestGeneratorsProto;
import io.spine.tools.protoc.plugin.method.TestMethodProtos;
import io.spine.type.MessageType;
import io.spine.validate.ValidatingBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.tools.mc.java.protoc.given.CodeGeneratorRequestGiven.addInterface;
import static io.spine.tools.mc.java.protoc.given.CodeGeneratorRequestGiven.generateMethods;
import static io.spine.tools.mc.java.protoc.given.CodeGeneratorRequestGiven.generateNested;
import static io.spine.tools.mc.java.protoc.given.CodeGeneratorRequestGiven.methodFactory;
import static io.spine.tools.mc.java.protoc.given.CodeGeneratorRequestGiven.pattern;
import static io.spine.tools.mc.java.protoc.given.CodeGeneratorRequestGiven.protocConfig;
import static io.spine.tools.mc.java.protoc.given.CodeGeneratorRequestGiven.requestBuilder;
import static io.spine.tools.mc.java.protoc.given.TestMethodFactory.TEST_METHOD;
import static io.spine.tools.mc.java.protoc.given.TestNestedClassFactory.NESTED_CLASS;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("`Plugin` should")
final class PluginTest {

    private static final String TEST_PROTO_SUFFIX = "_generators.proto";
    private static final String TEST_PROTO_PREFIX = "spine/tools/protoc/test_";
    private static final String TEST_PROTO_REGEX = ".*protoc/.*rators.pro.*";
    private static final String TEST_PROTO_FILE = "spine/tools/protoc/test_generators.proto";
    private static final String TEST_MESSAGE_TYPE_PARAMETER = "<EnhancedWithCodeGeneration>";
    private static final String BUILDER_INTERFACE =
            ValidatingBuilder.class.getName() + TEST_MESSAGE_TYPE_PARAMETER + ',';

    private Path testPluginConfig;

    @BeforeEach
    void setUp(@TempDir Path tempDirPath) {
        testPluginConfig = tempDirPath.resolve("test-spine-mc-java-protoc.pb");
    }

    @Test
    @DisplayName("generate UUID message")
    void generateUuidMethod() {
        var uuids = Uuids.newBuilder()
                .addMethodFactory(methodFactory(UuidMethodFactory.class))
                .build();
        var config = CodegenSettings.newBuilder()
                .setUuids(uuids)
                .build();
        var request = requestBuilder()
                .addProtoFile(TestMethodProtos.getDescriptor().toProto())
                .addFileToGenerate("spine/tools/protoc/method/test_protos.proto")
                .setParameter(protocConfig(config, testPluginConfig))
                .build();

        var response = runPlugin(request);
        var messageMethods =
                filterFiles(response, InsertionPoint.class_scope);
        assertThat(messageMethods)
                .hasSize(1);
    }

    @Test
    @DisplayName("process suffix patterns")
    void processSuffixPatterns() {
        var messages = MessageGroup.newBuilder()
                .setPattern(pattern(FilePatternFactory.INSTANCE.suffix(TEST_PROTO_SUFFIX)))
                .addAddInterface(addInterface(TestInterface.class))
                .addGenerateMethods(generateMethods(TestMethodFactory.class))
                .addGenerateNestedClasses(generateNested(TestNestedClassFactory.class))
                .build();
        var groupSettings = GroupSettings.newBuilder()
                .addGroup(messages)
                .build();
        var config = CodegenSettings.newBuilder()
                .setGroupSettings(groupSettings)
                .build();
        var request = requestBuilder()
                .addProtoFile(TestGeneratorsProto.getDescriptor()
                                                 .toProto())
                .addFileToGenerate(TEST_PROTO_FILE)
                .setParameter(protocConfig(config, testPluginConfig))
                .build();

        var response = runPlugin(request);
        checkGenerated(response);
    }

    @Test
    @DisplayName("process prefix patterns")
    void processPrefixPatterns() {
        var messages = MessageGroup.newBuilder()
                .setPattern(pattern(FilePatternFactory.INSTANCE.prefix(TEST_PROTO_PREFIX)))
                .addAddInterface(addInterface(TestInterface.class))
                .addGenerateMethods(generateMethods(TestMethodFactory.class))
                .addGenerateNestedClasses(generateNested(TestNestedClassFactory.class))
                .build();
        var groupSettings = GroupSettings.newBuilder()
                .addGroup(messages)
                .build();
        var config = CodegenSettings.newBuilder()
                .setGroupSettings(groupSettings)
                .build();
        var request = requestBuilder()
                .addProtoFile(TestGeneratorsProto.getDescriptor().toProto())
                .addFileToGenerate(TEST_PROTO_FILE)
                .setParameter(protocConfig(config, testPluginConfig))
                .build();

        var response = runPlugin(request);
        checkGenerated(response);
    }

    @Test
    @DisplayName("process regex patterns")
    void processRegexPatterns() {
        var messages = MessageGroup.newBuilder()
                .setPattern(pattern(FilePatternFactory.INSTANCE.regex(TEST_PROTO_REGEX)))
                .addAddInterface(addInterface(TestInterface.class))
                .addGenerateMethods(generateMethods(TestMethodFactory.class))
                .addGenerateNestedClasses(generateNested(TestNestedClassFactory.class))
                .build();
        var groupSettings = GroupSettings.newBuilder()
                .addGroup(messages)
                .addGroup(messages)
                .build();
        var config = CodegenSettings.newBuilder()
                .setGroupSettings(groupSettings)
                .build();
        var request = requestBuilder()
                .addProtoFile(TestGeneratorsProto.getDescriptor().toProto())
                .addFileToGenerate(TEST_PROTO_FILE)
                .setParameter(protocConfig(config, testPluginConfig))
                .build();

        var response = runPlugin(request);
        checkGenerated(response);
    }

    @Test
    @DisplayName("mark generated message builders with the `ValidatingBuilder` interface")
    void markBuildersWithInterface() {
        var testGeneratorsDescriptor = TestGeneratorsProto.getDescriptor();
        var config = CodegenSettings.getDefaultInstance();
        var protocConfigPath = protocConfig(config, testPluginConfig);
        var request = requestBuilder()
                .addProtoFile(testGeneratorsDescriptor.toProto())
                .addFileToGenerate(TEST_PROTO_FILE)
                .setParameter(protocConfigPath)
                .build();
        var response = runPlugin(request);

        var type = new MessageType(EnhancedWithCodeGeneration.getDescriptor());
        var expectedPointName = InsertionPoint.builder_implements.forType(type);

        var expectedSourceFile = SourceFile.forType(type);
        @SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
        var expectedFileName = expectedSourceFile.toString()
                                                 .replaceAll("\\\\", "/");
        var expectedFile = File.newBuilder()
                .setName(expectedFileName)
                .setInsertionPoint(expectedPointName)
                .setContent(BUILDER_INTERFACE)
                .build();
        assertThat(response.getFileList()).contains(expectedFile);
    }

    /**
     * Selects all files from the given response which contain the specified insertion point.
     */
    private static List<File> filterFiles(CodeGeneratorResponse response,
                                          InsertionPoint insertionPoint) {
        return response
                .getFileList()
                .stream()
                .filter(file -> file.getInsertionPoint()
                                    .contains(insertionPoint.getDefinition()))
                .collect(toList());
    }

    @SuppressWarnings("ZeroLengthArrayAllocation")
    private static CodeGeneratorResponse runPlugin(CodeGeneratorRequest request) {
        try (InputStream testInput = new ByteArrayInputStream(request.toByteArray());
             var bos = new ByteArrayOutputStream();
             var testOutput = new PrintStream(bos)
        ) {
            withSystemStreams(testInput, testOutput, () -> Plugin.main(new String[]{}));
            return CodeGeneratorResponse.parseFrom(bos.toByteArray(),
                                                   OptionsProvider.registryWithAllOptions());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr") // Required by the protoc API.
    private static void withSystemStreams(InputStream in, PrintStream os, Runnable action) {
        var oldIn = System.in;
        var oldOut = System.out;
        try {
            System.setIn(in);
            System.setOut(os);
            action.run();
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
    }

    private static void checkGenerated(CodeGeneratorResponse response) {
        var responseFiles = response.getFileList();
        var interfaceFile = responseFiles.stream()
                .filter(file -> file.getInsertionPoint().contains("implements"))
                .findFirst()
                .orElseGet(() -> fail("Expected an interface insertion point."));
        var classScopeFile = responseFiles.stream()
                .filter(file -> file.getInsertionPoint().contains("class_scope"))
                .findFirst()
                .orElseGet(() -> fail("Expected a class scope insertion point."));
        assertThat(interfaceFile.getContent())
                .contains(TestInterface.class.getName());
        var assertClassScope = assertThat(classScopeFile.getContent());
        assertClassScope
                .contains(TEST_METHOD.toString());
        assertClassScope
                .contains(NESTED_CLASS.toString());
    }
}
