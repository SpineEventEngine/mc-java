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

package io.spine.tools.mc.java.protoc.message;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;
import com.google.protobuf.compiler.PluginProtos.Version;
import io.spine.code.java.PackageName;
import io.spine.tools.java.fs.FileName;
import io.spine.tools.java.fs.JavaFiles;
import io.spine.tools.java.fs.SourceFile;
import io.spine.tools.mc.java.gradle.settings.CodegenSettings;
import io.spine.tools.mc.java.protoc.CodeGenerator;
import io.spine.tools.mc.java.settings.Combined;
import io.spine.tools.protoc.plugin.message.tests.EveryIsGeneratedProto;
import io.spine.tools.protoc.plugin.message.tests.EveryIsInOneFileProto;
import io.spine.tools.protoc.plugin.message.tests.EveryIsTestProto;
import io.spine.tools.protoc.plugin.message.tests.IsGeneratedProto;
import io.spine.tools.protoc.plugin.message.tests.IsInOneFileProto;
import io.spine.tools.protoc.plugin.message.tests.IsTestProto;
import io.spine.tools.protoc.plugin.message.tests.UserNameProto;
import io.spine.tools.protoc.plugin.message.tests.UserProto;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.regex.Pattern;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.Assertions.assertIllegalArgument;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`InterfaceGen` should")
@Disabled("During migration to new codegen")
final class InterfaceGenTest {

    private static final String INSERTION_POINT_IMPLEMENTS = "message_implements:%s";

    private static final String PROTO_PACKAGE = "spine.tools.protoc.msg";
    private static final String PROTO_DIR = PROTO_PACKAGE.replace('.', '/');

    /**
     * The package name of the test data, as declared in proto files under
     * the {@code proto/spine/tools/protoc/msg} directory.
     */
    private static final PackageName JAVA_PACKAGE =
            PackageName.of("io.spine.tools.protoc.plugin.message.tests");

    /**
     * The directory name containing the code generated from proto test data from
     * the {@code proto/spine/tools/protoc/msg} directory.
     *
     * @see #JAVA_PACKAGE
     */
    private static final String JAVA_DIR = JAVA_PACKAGE.value().replace('.', '/');

    /**
     * The pattern for the detecting {@linkplain #JAVA_PACKAGE test data package}.
     */
    private static final String PACKAGE_PATTERN =
            "^\\s*" + JAVA_PACKAGE.value().replace(".", "\\.");

    private static final Pattern CUSTOMER_EVENT_INTERFACE_PATTERN =
            compile(PACKAGE_PATTERN + "\\.ProtocCustomerEvent\\s*,\\s*$");

    private static final Pattern PROJECT_EVENT_INTERFACE_PATTERN =
            compile(PACKAGE_PATTERN + "\\.ProtocProjectEvent\\s*,\\s*$");

    private static final Pattern PROJECT_EVENT_INTERFACE_DECL_PATTERN =
            compile("public\\s+interface\\s+ProtocProjectEvent\\s*extends\\s+Message\\s*\\{\\s*}");

    private static final Pattern CUSTOMER_EVENT_OR_COMMAND =
            compile("Customer(Command|Event)");

    private CodeGenerator codeGenerator;

    private static String protoFile(String shortName) {
        return PROTO_DIR + '/' + shortName;
    }

    private static String javaFile(String shortName) {
        return JAVA_DIR + '/' + shortName;
    }

    private static Version version() {
        return Version.newBuilder()
                      .setMajor(3)
                      .setMinor(6)
                      .setPatch(1)
                      .build();
    }

    private static Combined settings = Combined.getDefaultInstance();

    @BeforeAll
    static void getDefaultSettings() {
        var project = ProjectBuilder.builder()
                .withName("Test")
                .build();
        project.getPluginManager()
               .apply("java");
        settings = new CodegenSettings(project).toProto();
    }

    @BeforeEach
    void setUp() {
        codeGenerator = InterfaceGen.instance(settings);
    }

    @Test
    @DisplayName("not accept nulls")
    void notAcceptNulls() {
        new NullPointerTester()
                .setDefault(CodeGeneratorRequest.class, CodeGeneratorRequest.getDefaultInstance())
                .testAllPublicStaticMethods(InterfaceGen.class);
    }

    private static String messageNameFrom(File file) {
        var fileName = file.getName();
        var messageName = PROTO_PACKAGE + '.' +
                fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf('.'));
        return messageName;
    }

    @Nested
    @DisplayName("generate insertion point contents for")
    class InsertionPoints {

        @Test
        @DisplayName("`EveryIs` option")
        void everyIsOption() {
            var filePath = protoFile("every_is_test.proto");

            var fileDescr = EveryIsTestProto.getDescriptor().toProto();
            var response = processCodeGenRequest(filePath, fileDescr);
            assertNotNull(response);
            var files = response.getFileList();
            assertEquals(2, files.size());
            for (var file : files) {
                assertPackage(file);

                var messageName = messageNameFrom(file);
                var insertionPoint = file.getInsertionPoint();
                assertThat(insertionPoint)
                        .isEqualTo(format(INSERTION_POINT_IMPLEMENTS, messageName));
                var content = file.getContent();
                assertThat(content)
                        .matches(CUSTOMER_EVENT_INTERFACE_PATTERN);
            }
        }

        @Test
        @DisplayName("`Is` option")
        void generateInsertionPointContentsForIsOption() {
            var filePath = protoFile("is_test.proto");

            var fileDescr =
                    IsTestProto.getDescriptor()
                               .toProto();
            var response = processCodeGenRequest(filePath, fileDescr);
            assertNotNull(response);
            var files = response.getFileList();
            assertEquals(2, files.size());
            for (var file : files) {
                assertPackage(file);

                var name = file.getName();
                var insertionPoint = file.getInsertionPoint();
                assertFalse(insertionPoint.isEmpty());
                var content = file.getContent();
                var assertContent = assertThat(content);
                if (name.endsWith("ProtocNameUpdated.java")) {
                    assertContent.contains("Event,");
                } else if (name.endsWith("ProtocUpdateName.java")) {
                    assertContent.contains("Command,");
                }
            }
        }

        @Test
        @DisplayName("`EveryIs` in single file")
        void generateInsertionPointContentsForEveryIsInSingleFile() {
            var filePath = protoFile("every_is_in_one_file.proto");

            var fileDescr = EveryIsInOneFileProto.getDescriptor().toProto();
            var response = processCodeGenRequest(filePath, fileDescr);
            assertNotNull(response);
            var files = response.getFileList();
            assertEquals(2, files.size());
            for (var file : files) {
                if (!haveSamePath(file, sourceWithPackage("ProtocCustomerEvent"))) {
                    assertFilePath(file, sourceWithPackage("EveryIsInOneFileProto"));

                    var insertionPoint = file.getInsertionPoint();
                    assertThat(insertionPoint)
                            .startsWith(format(INSERTION_POINT_IMPLEMENTS, PROTO_PACKAGE));
                    var content = file.getContent();
                    assertThat(content)
                            .matches(CUSTOMER_EVENT_INTERFACE_PATTERN);
                }
            }
        }

        @Test
        @DisplayName("`Is` in single file")
        void isInSingleFile() {
            var filePath = protoFile("is_in_one_file.proto");

            var fileDescr = IsInOneFileProto.getDescriptor().toProto();
            var response = processCodeGenRequest(filePath, fileDescr);
            assertNotNull(response);
            var files = response.getFileList();
            assertEquals(2, files.size());
            for (var file : files) {
                assertFilePath(file, sourceWithPackage("IsInOneFileProto"));

                var insertionPoint = file.getInsertionPoint();
                assertThat(insertionPoint)
                        .startsWith(format(INSERTION_POINT_IMPLEMENTS, PROTO_PACKAGE));
                var content = file.getContent();
                assertThat(content)
                        .matches(CUSTOMER_EVENT_INTERFACE_PATTERN);
            }
        }
    }

    @Test
    @DisplayName("not accept requests from old compiler")
    void notAcceptRequestsFromOldCompiler() {
        var version = Version.newBuilder()
                .setMajor(2)
                .build();
        var stubFile = FileDescriptorProto.getDefaultInstance();
        var request = CodeGeneratorRequest.newBuilder()
                .setCompilerVersion(version)
                .addProtoFile(stubFile)
                .build();
        assertIllegalArgument(() -> codeGenerator.process(request));
    }

    @Test
    @DisplayName("not accept empty requests")
    void notAcceptEmptyRequests() {
        var version = Version.newBuilder()
                .setMajor(3)
                .build();
        var request = CodeGeneratorRequest.newBuilder()
                .setCompilerVersion(version)
                .build();
        assertIllegalArgument(() -> codeGenerator.process(request));
    }

    @Nested
    @DisplayName("generate message interfaces for")
    class Interfaces {

        @Test
        @DisplayName("`(is)` if `generate = true`")
        void forIs() {
            var filePath = protoFile("is_generated.proto");

            var fileDescr = IsGeneratedProto.getDescriptor().toProto();
            var response = processCodeGenRequest(filePath, fileDescr);
            assertNotNull(response);
            var files = response.getFileList();
            assertEquals(4, files.size());
            for (var file : files) {
                assertPackage(file);

                var fileName = file.getName();
                var insertionPoint = file.getInsertionPoint();
                if (!insertionPoint.isEmpty()) {
                    var messageName = messageNameFrom(file);
                    assertThat(insertionPoint)
                            .isEqualTo(format(INSERTION_POINT_IMPLEMENTS, messageName));
                }

                var content = file.getContent();
                var assertContent = assertThat(content);
                if (fileName.endsWith("ProtocSurnameUpdated.java")) {
                    assertContent.contains("Event,");
                } else if (fileName.endsWith("ProtocUpdateSurname.java")) {
                    assertContent.contains("Command,");
                } else {
                    assertTrue(CUSTOMER_EVENT_OR_COMMAND.matcher(fileName)
                                                        .find());
                }
            }
        }

        @Test
        @DisplayName("`(every_is)` if `generate = true`")
        void forEveryIs() {
            var filePath = protoFile("every_is_generated.proto");

            var fileDescr = EveryIsGeneratedProto.getDescriptor().toProto();
            var response = processCodeGenRequest(filePath, fileDescr);
            assertNotNull(response);
            var files = response.getFileList();
            assertEquals(3, files.size());
            for (var file : files) {
                assertPackage(file);

                var content = file.getContent();
                var insertionPoint = file.getInsertionPoint();
                if (!insertionPoint.isEmpty()) {
                    var messageName = messageNameFrom(file);
                    assertThat(insertionPoint)
                            .isEqualTo(format(INSERTION_POINT_IMPLEMENTS, messageName));

                    var matcher = PROJECT_EVENT_INTERFACE_PATTERN.matcher(content);
                    assertTrue(matcher.matches());
                } else {
                    var matcher = PROJECT_EVENT_INTERFACE_DECL_PATTERN.matcher(content);
                    assertTrue(matcher.find());
                }
            }
        }
    }

    @Test
    @DisplayName("skip generation for types included in compilation but not requested to be generated")
    void skipIncluded() {
        var requestedTypes = UserProto.getDescriptor().toProto();
        var includedTypes = UserNameProto.getDescriptor().toProto();
        var request = CodeGeneratorRequest.newBuilder()
                .setCompilerVersion(version())
                .addFileToGenerate(protoFile("user.proto"))
                .addProtoFile(requestedTypes)
                .addProtoFile(includedTypes)
                .build();
        var response = codeGenerator.process(request);
        var generatedFiles = response.getFileList().stream()
                .map(File::getName)
                .collect(toSet());

        var assertFiles = assertThat(generatedFiles);
        assertFiles.doesNotContain(javaFile("UserName.java"));
        assertFiles.doesNotContain(javaFile("Name.java"));
        assertFiles.containsExactly(
                javaFile("User.java"),
                javaFile("LawSubject.java")
        );
    }

    private CodeGeneratorResponse
    processCodeGenRequest(String filePath, FileDescriptorProto descriptor) {
        var request = CodeGeneratorRequest.newBuilder()
                .setCompilerVersion(version())
                .addFileToGenerate(filePath)
                .addProtoFile(descriptor)
                .build();
        return codeGenerator.process(request);
    }

    private static SourceFile sourceWithPackage(String typeName) {
        var fileName = FileName.forType(typeName);
        var packageDir = JavaFiles.toDirectory(JAVA_PACKAGE);
        return JavaFiles.resolve(packageDir, fileName);
    }

    private static boolean haveSamePath(File generatedFile, SourceFile anotherFile) {
        var generatedFilePath = Paths.get(generatedFile.getName());
        return generatedFilePath.equals(anotherFile.path());
    }

    private static void assertFilePath(File generatedFile, SourceFile expectedFile) {
        assertTrue(haveSamePath(generatedFile, expectedFile));
    }

    private static void assertPackage(File generatedFile) {
        var generatedFilePath = Paths.get(generatedFile.getName());
        var directory = JavaFiles.toDirectory(JAVA_PACKAGE);
        assertThat(generatedFilePath.toString())
                .startsWith(directory.toString());
    }

    /**
     * Verifies that the file contains the name of the interface class suffixed with comma.
     *
     * <p>The trailing comma is needed because the interface name will be one of the several
     * interfaces in the {@code implements} clause of the generated class.
     */
    private static void assertGeneratedInterface(Class<?> interfaceClass, File file) {
        assertTrue(file.hasInsertionPoint());
        assertTrue(file.hasName());
        var assertContent = assertThat(file.getContent());
        assertContent.startsWith(interfaceClass.getName());
        assertContent.endsWith(",");
    }
}
