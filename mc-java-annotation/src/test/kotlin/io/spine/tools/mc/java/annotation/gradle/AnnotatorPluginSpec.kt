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

@file:Suppress("TooManyFunctions")

package io.spine.tools.mc.java.annotation.gradle

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Descriptors.FileDescriptor
import io.spine.annotation.Internal
import io.spine.annotation.SPI
import io.spine.code.proto.FileDescriptors.DESC_EXTENSION
import io.spine.code.proto.FileName
import io.spine.code.proto.FileSet
import io.spine.tools.div
import io.spine.tools.fs.DirectoryName.build
import io.spine.tools.fs.DirectoryName.descriptors
import io.spine.tools.fs.DirectoryName.grpc
import io.spine.tools.fs.DirectoryName.java
import io.spine.tools.gradle.task.BaseTaskName
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.gradle.testing.get
import io.spine.tools.java.fs.SourceFile
import io.spine.tools.mc.java.annotation.check.FieldAnnotationCheck
import io.spine.tools.mc.java.annotation.check.MainDefinitionAnnotationCheck
import io.spine.tools.mc.java.annotation.check.NestedTypeFieldsAnnotationCheck
import io.spine.tools.mc.java.annotation.check.NestedTypesAnnotationCheck
import io.spine.tools.mc.java.annotation.check.SourceCheck
import io.spine.tools.mc.java.annotation.given.GivenProtoFile
import io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_ALL
import io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_ALL_MULTIPLE
import io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_ALL_SERVICE
import io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_FIELD
import io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_FIELD_MULTIPLE
import io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_MESSAGE
import io.spine.tools.mc.java.annotation.given.GivenProtoFile.INTERNAL_MESSAGE_MULTIPLE
import io.spine.tools.mc.java.annotation.given.GivenProtoFile.NO_INTERNAL_OPTIONS
import io.spine.tools.mc.java.annotation.given.GivenProtoFile.NO_INTERNAL_OPTIONS_MULTIPLE
import io.spine.tools.mc.java.annotation.given.GivenProtoFile.SPI_SERVICE
import io.spine.tools.mc.java.annotation.gradle.AnnotatorPluginSpec.Companion.moduleDir
import io.spine.tools.mc.java.gradle.McJavaTaskName.Companion.annotateProto
import java.io.File
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.name
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.impl.AbstractJavaSource
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@Disabled("Until rejections are fully migrated to ProtoData.")
@DisplayName("`AnnotatorPlugin` should")
internal class AnnotatorPluginSpec {

    companion object {
        private const val RESOURCE_DIR = "annotator-plugin-test"
        private const val RESOURCE_SUB_DIR = "typedefs"
        lateinit var moduleDir: Path

        @BeforeAll
        @JvmStatic
        fun compileProject(@TempDir projectDir: File) {
            val project = GradleProject.setupAt(projectDir)
                .fromResources(RESOURCE_DIR)
                .copyBuildSrc()
                /* Uncomment the following line to be able to debug the build.
                   Do not forget to turn off so that tests run faster AND Windows build does not
                    fail with the error on Windows Registry unavailability. */
                //.enableRunnerDebug()
                .create()
            moduleDir = projectDir.toPath() / RESOURCE_SUB_DIR
            project.executeTask(annotateProto)
        }
    }

    @Nested
    @DisplayName("annotate")
    internal inner class Annotating {

        @Test
        fun `if file option is true`() =
            checkNestedTypesAnnotations(INTERNAL_ALL, true)

        @Test
        fun `service if file option if true`() =
            checkServiceAnnotations(INTERNAL_ALL_SERVICE, true)

        @Test
        fun `multiple files if file option is true`() =
            checkMainDefinitionAnnotations(INTERNAL_ALL_MULTIPLE, true)

        @Test
        fun `if message option is true`() =
            checkNestedTypesAnnotations(INTERNAL_MESSAGE, true)

        @Test
        fun `multiple files if message option is true`() =
            checkMainDefinitionAnnotations(INTERNAL_MESSAGE_MULTIPLE, true)

        @Test
        fun `accessors if field option is true`() =
            checkFieldAnnotations(INTERNAL_FIELD, true)

        @Test
        fun `accessors in multiple files if field option is true`() =
            checkFieldAnnotationsMultiple(INTERNAL_FIELD_MULTIPLE, true)

        @Test
        fun `GRPC services if service option is true`() =
            checkServiceAnnotations(SPI_SERVICE.fileName(), SPI::class.java, true)
    }

    @Nested
    @DisplayName("not annotate")
    internal inner class NotAnnotate {

        @Test
        fun `if file option if false`() =
            checkNestedTypesAnnotations(NO_INTERNAL_OPTIONS, false)

        @Test
        fun `service if file option is false`() =
            checkNestedTypesAnnotations(NO_INTERNAL_OPTIONS, false)

        @Test
        fun `multiple files if file option is false`() =
            checkMainDefinitionAnnotations(NO_INTERNAL_OPTIONS_MULTIPLE, false)

        @Test
        fun `if message option is false`() =
            checkNestedTypesAnnotations(NO_INTERNAL_OPTIONS, false)

        @Test
        fun `multiple files if message option is false`() =
            checkMainDefinitionAnnotations(NO_INTERNAL_OPTIONS_MULTIPLE, false)

        @Test
        fun `accessors if field option is false`() =
            checkFieldAnnotations(NO_INTERNAL_OPTIONS, false)

        @Test
        fun `accessors in multiple files if field option is false`() =
            checkFieldAnnotationsMultiple(NO_INTERNAL_OPTIONS_MULTIPLE, false)

        @Test
        fun `GRPC services if service option is false`() =
            checkServiceAnnotations(NO_INTERNAL_OPTIONS, false)
    }

    @Test
    fun `compile generated source with potential annotation duplication`(@TempDir tempDir: File) {
        val project = GradleProject.setupAt(tempDir)
            .fromResources(RESOURCE_DIR)
            .copyBuildSrc()
            .create()
        val result = project.executeTask(BaseTaskName.build)
        assertThat(result[BaseTaskName.build]).isEqualTo(SUCCESS)
    }
}

private val Path.generatedFromProto: Path
    get() = this / "build/generated/source/proto/main"

private fun checkServiceAnnotations(testFile: GivenProtoFile, shouldBeAnnotated: Boolean) =
    checkServiceAnnotations(testFile.fileName(), Internal::class.java, shouldBeAnnotated)

private fun checkServiceAnnotations(
    testFile: FileName,
    expectedAnnotation: Class<out Annotation>,
    shouldBeAnnotated: Boolean
) {
    val fileDescriptor = descriptorOf(testFile)
    val services = fileDescriptor.services
    for (serviceDescriptor in services) {
        val serviceFile = SourceFile.forService(serviceDescriptor)
        val check = MainDefinitionAnnotationCheck(expectedAnnotation, shouldBeAnnotated)
        check.verifyService(serviceFile)
    }
}

private fun checkFieldAnnotations(testFile: GivenProtoFile, shouldBeAnnotated: Boolean) {
    val fileDescriptor = descriptorOf(testFile.fileName())
    val messageDescriptor = fileDescriptor.messageTypes[0]
    val sourcePath = SourceFile.forMessage(messageDescriptor).path()

    NestedTypeFieldsAnnotationCheck(messageDescriptor, shouldBeAnnotated).verify(sourcePath)
}

private fun checkFieldAnnotationsMultiple(
    testFile: GivenProtoFile,
    shouldBeAnnotated: Boolean
) {
    val fileDescriptor = descriptorOf(testFile.fileName())
    val messageDescriptor = fileDescriptor.messageTypes[0]
    val experimentalField = messageDescriptor.fields[0]
    val sourcePath = SourceFile.forMessage(messageDescriptor).path()

    FieldAnnotationCheck(experimentalField, shouldBeAnnotated).verify(sourcePath)
}

private fun checkMainDefinitionAnnotations(
    testFile: GivenProtoFile,
    shouldBeAnnotated: Boolean
) {
    val fileDescriptor = descriptorOf(testFile.fileName())
    for (messageDescriptor in fileDescriptor.messageTypes) {
        val messageProto = messageDescriptor.toProto()
        val fileProto = fileDescriptor.toProto()
        val messagePath = SourceFile.forMessage(messageProto, fileProto).path()

        MainDefinitionAnnotationCheck(shouldBeAnnotated).verify(messagePath)
    }
}

private fun checkNestedTypesAnnotations(
    testFile: GivenProtoFile,
    shouldBeAnnotated: Boolean
) {
    val fileDescriptor = descriptorOf(testFile.fileName())
    val sourcePath = SourceFile.forOuterClassOf(fileDescriptor.toProto()).path()

    NestedTypesAnnotationCheck(shouldBeAnnotated).verify(sourcePath)
}

@Suppress("UNCHECKED_CAST")
private fun parse(file: Path): AbstractJavaSource<JavaClassSource> {
    val javaSource = Roaster.parse(AbstractJavaSource::class.java, file.toFile())
    return javaSource as AbstractJavaSource<JavaClassSource>
}

private fun SourceCheck.verify(sourcePath: Path) {
    val filePath = moduleDir.generatedFromProto / java / sourcePath
    val javaSource = parse(filePath)
    accept(javaSource)
}

private fun SourceCheck.verifyService(serviceFile: SourceFile) {
    val filePath = moduleDir.generatedFromProto / grpc / serviceFile.path()
    val javaSource = parse(filePath)
    accept(javaSource)
}

private fun descriptorOf(testFile: FileName): FileDescriptor {
    val mainDescriptor = mainDescriptorPath()
    val fileSet = FileSet.parse(mainDescriptor.toFile())
    val file = fileSet.tryFind(testFile)
    check(file.isPresent) { "Unable to get file descriptor for `$testFile`." }
    return file.get()
}

/**
 * Composes the path to the main descriptor set file using the project Maven coordinates
 * as defined in the test project under `resources/annotator-plugin-test`.
 */
private fun mainDescriptorPath(): Path =
    moduleDir / build / descriptors / MAIN_SOURCE_SET_NAME /
            "io.spine.test_${moduleDir.name}_3.14$DESC_EXTENSION"
