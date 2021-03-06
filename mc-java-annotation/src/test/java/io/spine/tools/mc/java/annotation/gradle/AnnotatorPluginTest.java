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

package io.spine.tools.mc.java.annotation.gradle;

import com.google.protobuf.Descriptors.FileDescriptor;
import io.spine.annotation.Internal;
import io.spine.annotation.SPI;
import io.spine.code.proto.FileName;
import io.spine.code.proto.FileSet;
import io.spine.testing.TempDir;
import io.spine.tools.gradle.testing.GradleProject;
import io.spine.tools.java.fs.DefaultJavaPaths;
import io.spine.tools.java.fs.SourceFile;
import io.spine.tools.mc.java.annotation.check.FieldAnnotationCheck;
import io.spine.tools.mc.java.annotation.check.MainDefinitionAnnotationCheck;
import io.spine.tools.mc.java.annotation.check.NestedTypeFieldsAnnotationCheck;
import io.spine.tools.mc.java.annotation.check.NestedTypesAnnotationCheck;
import io.spine.tools.mc.java.annotation.check.SourceCheck;
import io.spine.tools.mc.java.annotation.given.GivenProtoFile;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.impl.AbstractJavaSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Path;

import static com.google.common.base.Preconditions.checkState;
import static io.spine.code.proto.FileDescriptors.DESC_EXTENSION;
import static io.spine.tools.code.SourceSetName.main;
import static io.spine.tools.gradle.task.JavaTaskName.compileJava;
import static io.spine.tools.java.fs.SourceFile.forMessage;
import static io.spine.tools.java.fs.SourceFile.forOuterClassOf;
import static io.spine.tools.java.fs.SourceFile.forService;
import static io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_ALL;
import static io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_ALL_MULTIPLE;
import static io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_ALL_SERVICE;
import static io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_FIELD;
import static io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_FIELD_MULTIPLE;
import static io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_MESSAGE;
import static io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_MESSAGE_MULTIPLE;
import static io.spine.tools.mc.java.annotation.given.GivenProtoFile.NO_INTERNAL_OPTIONS;
import static io.spine.tools.mc.java.annotation.given.GivenProtoFile.NO_INTERNAL_OPTIONS_MULTIPLE;
import static io.spine.tools.mc.java.annotation.given.GivenProtoFile.SPI_SERVICE;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.annotateProto;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

@DisplayName("`AnnotatorPlugin` should")
class AnnotatorPluginTest {

    private static final String RESOURCE_DIR = "annotator-plugin-test";

    private static File moduleDir = null;

    @BeforeAll
    static void compileProject() {
        var projectDir = TempDir.forClass(AnnotatorPluginTest.class);
        var project = GradleProject.setupAt(projectDir)
                                   .fromResources(RESOURCE_DIR)
                                   .copyBuildSrc().create();
        moduleDir = projectDir.toPath()
                              .resolve("tests")
                              .toFile();
        project.executeTask(annotateProto);
    }

    @Nested
    @DisplayName("annotate")
    class Annotating {

        @Test
        @DisplayName("if file option is true")
        void ifFileOptionSet() throws IOException {
            checkNestedTypesAnnotations(INTERNAL_ALL, true);
        }

        @Test
        @DisplayName("service if file option if true")
        void serviceIfFileOptionSet() throws IOException {
            checkServiceAnnotations(INTERNAL_ALL_SERVICE, true);
        }

        @Test
        @DisplayName("multiple files if file option is true")
        void multipleFilesIfFileOptionSet() throws IOException {
            checkMainDefinitionAnnotations(INTERNAL_ALL_MULTIPLE, true);
        }

        @Test
        @DisplayName("if message option is true")
        void ifMessageOptionSet() throws IOException {
            checkNestedTypesAnnotations(INTERNAL_MESSAGE, true);
        }

        @Test
        @DisplayName("multiple files if message option is true")
        void multipleFiles() throws IOException {
            checkMainDefinitionAnnotations(INTERNAL_MESSAGE_MULTIPLE, true);
        }

        @Test
        @DisplayName("accessors if field option is true")
        void accessors() throws IOException {
            checkFieldAnnotations(INTERNAL_FIELD, true);
        }

        @Test
        @DisplayName("accessors in multiple files if field option is true")
        void accessorsInMultipleFiles() throws IOException {
            checkFieldAnnotationsMultiple(INTERNAL_FIELD_MULTIPLE, true);
        }

        @Test
        @DisplayName("GRPC services if service option is true")
        void grpcServices() throws IOException {
            checkServiceAnnotations(SPI_SERVICE.fileName(), SPI.class, true);
        }
    }

    @Nested
    @DisplayName("not annotate")
    class NotAnnotating {

        @Test
        @DisplayName("if file option if false")
        void ifFileOption() throws IOException {
            checkNestedTypesAnnotations(NO_INTERNAL_OPTIONS, false);
        }

        @Test
        @DisplayName("service if file option is false")
        void serviceIfFileOption() throws IOException {
            checkNestedTypesAnnotations(NO_INTERNAL_OPTIONS, false);
        }

        @Test
        @DisplayName("multiple files if file option is false")
        void multipleFilesIfFileOption() throws IOException {
            checkMainDefinitionAnnotations(NO_INTERNAL_OPTIONS_MULTIPLE, false);
        }

        @Test
        @DisplayName("if message option is false")
        void ifMessageOption() throws IOException {
            checkNestedTypesAnnotations(NO_INTERNAL_OPTIONS, false);
        }

        @Test
        @DisplayName("multiple files if message option is false")
        void multipleFiles() throws IOException {
            checkMainDefinitionAnnotations(NO_INTERNAL_OPTIONS_MULTIPLE, false);
        }

        @Test
        @DisplayName("accessors if field option is false")
        void accessors() throws IOException {
            checkFieldAnnotations(NO_INTERNAL_OPTIONS, false);
        }

        @Test
        @DisplayName("accessors in multiple files if field option is false")
        void accessorsInMultipleFiles() throws IOException {
            checkFieldAnnotationsMultiple(NO_INTERNAL_OPTIONS_MULTIPLE, false);
        }

        @Test
        @DisplayName("GRPC services if service option is false")
        void gprcServices() throws IOException {
            checkServiceAnnotations(NO_INTERNAL_OPTIONS, false);
        }
    }

    @Test
    @DisplayName("compile generated source with potential annotation duplication")
    void compilingSources() {
        var tempDir = TempDir.forClass(AnnotatorPluginTest.class);
        var project = GradleProject.setupAt(tempDir)
                .fromResources(RESOURCE_DIR)
                .copyBuildSrc()
                .create();
        project.executeTask(compileJava);
    }

    private static void checkServiceAnnotations(GivenProtoFile testFile, boolean shouldBeAnnotated)
            throws IOException {
        checkServiceAnnotations(testFile.fileName(), Internal.class, shouldBeAnnotated);
    }

    private static void checkServiceAnnotations(FileName testFile,
                                                Class<? extends Annotation> expectedAnnotation,
                                                boolean shouldBeAnnotated)
            throws IOException {
        var fileDescriptor = descriptorOf(testFile);
        var services = fileDescriptor.getServices();
        for (var serviceDescriptor : services) {
            var serviceFile = forService(serviceDescriptor.toProto(), fileDescriptor.toProto());
            var check = new MainDefinitionAnnotationCheck(expectedAnnotation, shouldBeAnnotated);
            checkGrpcService(serviceFile, check);
        }
    }

    private static void checkFieldAnnotations(GivenProtoFile testFile, boolean shouldBeAnnotated)
            throws IOException {
        var fileDescriptor = descriptorOf(testFile.fileName());
        var messageDescriptor = fileDescriptor.getMessageTypes().get(0);
        var sourcePath = forMessage(messageDescriptor.toProto(), fileDescriptor.toProto()).path();
        var check = new NestedTypeFieldsAnnotationCheck(messageDescriptor, shouldBeAnnotated);
        check(sourcePath, check);
    }

    private static
    void checkFieldAnnotationsMultiple(GivenProtoFile testFile, boolean shouldBeAnnotated)
            throws IOException {
        var fileDescriptor = descriptorOf(testFile.fileName());
        var messageDescriptor = fileDescriptor.getMessageTypes().get(0);
        var experimentalField = messageDescriptor.getFields().get(0);
        var sourcePath = forMessage(messageDescriptor.toProto(), fileDescriptor.toProto()).path();
        check(sourcePath, new FieldAnnotationCheck(experimentalField, shouldBeAnnotated));
    }

    private static
    void checkMainDefinitionAnnotations(GivenProtoFile testFile, boolean shouldBeAnnotated)
            throws IOException {
        var fileDescriptor = descriptorOf(testFile.fileName());
        for (var messageDescriptor : fileDescriptor.getMessageTypes()) {
            var messageProto = messageDescriptor.toProto();
            var fileProto = fileDescriptor.toProto();
            var messagePath = forMessage(messageProto, fileProto).path();
            SourceCheck annotationCheck = new MainDefinitionAnnotationCheck(shouldBeAnnotated);
            check(messagePath, annotationCheck);
        }
    }

    private static
    void checkNestedTypesAnnotations(GivenProtoFile testFile, boolean shouldBeAnnotated)
            throws IOException {
        var fileDescriptor = descriptorOf(testFile.fileName());
        var sourcePath = forOuterClassOf(fileDescriptor.toProto()).path();
        check(sourcePath, new NestedTypesAnnotationCheck(shouldBeAnnotated));
    }

    private static void check(Path sourcePath, SourceCheck check) throws IOException {
        var filePath = DefaultJavaPaths.at(moduleDir)
                                       .generatedProto()
                                       .java(main)
                                       .path()
                                       .resolve(sourcePath);
        @SuppressWarnings("unchecked")
        AbstractJavaSource<JavaClassSource> javaSource =
                Roaster.parse(AbstractJavaSource.class, filePath.toFile());
        check.accept(javaSource);
    }

    private static void checkGrpcService(SourceFile serviceFile, SourceCheck check)
            throws IOException {
        var filePath = DefaultJavaPaths.at(moduleDir)
                                       .generatedProto()
                                       .grpc(main)
                                       .path()
                                       .resolve(serviceFile.path());
        @SuppressWarnings("unchecked")
        AbstractJavaSource<JavaClassSource> javaSource =
                Roaster.parse(AbstractJavaSource.class, filePath.toFile());
        check.accept(javaSource);
    }

    private static FileDescriptor descriptorOf(FileName testFile) {
        var mainDescriptor = mainDescriptorPath();
        var fileSet = FileSet.parse(mainDescriptor.toFile());
        var file = fileSet.tryFind(testFile);
        checkState(file.isPresent(), "Unable to get file descriptor for `%s`.", testFile);
        var result = file.get();
        return result;
    }

    /**
     * Compose the path to the main descriptor set file using the project Maven coordinates
     * as defined in the test project under {@code resources/annotator-plugin-test}.
     */
    private static Path mainDescriptorPath() {
        return DefaultJavaPaths.at(moduleDir)
                .buildRoot()
                .descriptors()
                .forSourceSet(MAIN_SOURCE_SET_NAME)
                .resolve("io.spine.test_" + moduleDir.getName() + "_3.14" + DESC_EXTENSION);
    }
}
