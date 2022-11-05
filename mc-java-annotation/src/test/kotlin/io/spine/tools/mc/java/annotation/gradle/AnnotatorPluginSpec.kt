package io.spine.tools.mc.java.annotation.gradle

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Descriptors.FileDescriptor
import io.spine.annotation.Internal
import io.spine.annotation.SPI
import io.spine.code.proto.FileDescriptors.DESC_EXTENSION
import io.spine.code.proto.FileName
import io.spine.code.proto.FileSet
import io.spine.tools.code.SourceSetName.Companion.main
import io.spine.tools.gradle.task.BaseTaskName.build
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.gradle.testing.get
import io.spine.tools.java.fs.DefaultJavaPaths
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
import io.spine.tools.mc.java.gradle.McJavaTaskName
import java.io.File
import java.nio.file.Path
import org.gradle.api.tasks.SourceSet
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.impl.AbstractJavaSource
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`AnnotatorPlugin` should")
internal class AnnotatorPluginSpec {

    companion object {
        private const val RESOURCE_DIR = "annotator-plugin-test"
        lateinit var moduleDir: File

        @BeforeAll
        @JvmStatic
        fun compileProject(@TempDir projectDir: File) {
            val project = GradleProject.setupAt(projectDir)
                .fromResources(RESOURCE_DIR)
                .copyBuildSrc()
                .create()
            moduleDir = projectDir.toPath().resolve("tests").toFile()
            project.executeTask(McJavaTaskName.annotateProto)
        }
    }

    @Nested
    @DisplayName("annotate")
    internal inner class Annotating {

        @Test
        fun `if file option is true`() {
            checkNestedTypesAnnotations(INTERNAL_ALL, true)
        }

        @Test
        fun `service if file option if true`() {
            checkServiceAnnotations(INTERNAL_ALL_SERVICE, true)
        }

        @Test
        fun `multiple files if file option is true`() {
            checkMainDefinitionAnnotations(INTERNAL_ALL_MULTIPLE, true)
        }

        @Test
        fun `if message option is true`() {
            checkNestedTypesAnnotations(INTERNAL_MESSAGE, true)
        }

        @Test
        fun `multiple files if message option is true`() {
            checkMainDefinitionAnnotations(INTERNAL_MESSAGE_MULTIPLE, true)
        }

        @Test
        fun `accessors if field option is true`() {
            checkFieldAnnotations(INTERNAL_FIELD, true)
        }

        @Test
        fun `accessors in multiple files if field option is true`() {
            checkFieldAnnotationsMultiple(INTERNAL_FIELD_MULTIPLE, true)
        }

        @Test
        fun `GRPC services if service option is true`() {
            checkServiceAnnotations(SPI_SERVICE.fileName(), SPI::class.java, true)
        }
    }

    @Nested
    internal inner class `not annotate` {

        @Test
        fun `if file option if false`() {
            checkNestedTypesAnnotations(NO_INTERNAL_OPTIONS, false)
        }

        @Test
        fun `service if file option is false`() {
            checkNestedTypesAnnotations(NO_INTERNAL_OPTIONS, false)
        }

        @Test
        fun `multiple files if file option is false`() {
            checkMainDefinitionAnnotations(NO_INTERNAL_OPTIONS_MULTIPLE, false)
        }

        @Test
        fun `if message option is false`() {
            checkNestedTypesAnnotations(NO_INTERNAL_OPTIONS, false)
        }

        @Test
        fun `multiple files if message option is false`() {
            checkMainDefinitionAnnotations(NO_INTERNAL_OPTIONS_MULTIPLE, false)
        }

        @Test
        fun `accessors if field option is false`() {
            checkFieldAnnotations(NO_INTERNAL_OPTIONS, false)
        }

        @Test
        fun `accessors in multiple files if field option is false`() {
            checkFieldAnnotationsMultiple(NO_INTERNAL_OPTIONS_MULTIPLE, false)
        }

        @Test
        fun `GRPC services if service option is false`() {
            checkServiceAnnotations(NO_INTERNAL_OPTIONS, false)
        }
    }

    @Test
    fun `compile generated source with potential annotation duplication`(@TempDir tempDir: File) {
        val project = GradleProject.setupAt(tempDir)
            .fromResources(RESOURCE_DIR)
            .copyBuildSrc()
            .create()
        val result = project.executeTask(build)
        assertThat(result[build]).isEqualTo(SUCCESS)
    }
}

private fun checkServiceAnnotations(testFile: GivenProtoFile, shouldBeAnnotated: Boolean) {
    checkServiceAnnotations(testFile.fileName(), Internal::class.java, shouldBeAnnotated)
}

private fun checkServiceAnnotations(
    testFile: FileName,
    expectedAnnotation: Class<out Annotation>,
    shouldBeAnnotated: Boolean
) {
    val fileDescriptor = descriptorOf(testFile)
    val services = fileDescriptor.services
    for (serviceDescriptor in services) {
        val serviceFile = SourceFile.forService(serviceDescriptor, fileDescriptor)
        val check = MainDefinitionAnnotationCheck(expectedAnnotation, shouldBeAnnotated)
        check.verifyService(serviceFile)
    }
}

private fun checkFieldAnnotations(testFile: GivenProtoFile, shouldBeAnnotated: Boolean) {
    val fileDescriptor = descriptorOf(testFile.fileName())
    val messageDescriptor = fileDescriptor.messageTypes[0]
    val sourcePath = SourceFile.forMessage(messageDescriptor, fileDescriptor).path()

    NestedTypeFieldsAnnotationCheck(messageDescriptor, shouldBeAnnotated).verify(sourcePath)
}

private fun checkFieldAnnotationsMultiple(
    testFile: GivenProtoFile,
    shouldBeAnnotated: Boolean
) {
    val fileDescriptor = descriptorOf(testFile.fileName())
    val messageDescriptor = fileDescriptor.messageTypes[0]
    val experimentalField = messageDescriptor.fields[0]
    val sourcePath = SourceFile.forMessage(messageDescriptor, fileDescriptor).path()

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
    val filePath = DefaultJavaPaths.at(moduleDir)
        .generatedProto()
        .java(main)
        .path()
        .resolve(sourcePath)
    val javaSource = parse(filePath)
    accept(javaSource)
}

private fun SourceCheck.verifyService(serviceFile: SourceFile) {
    val filePath = DefaultJavaPaths.at(moduleDir)
        .generatedProto()
        .grpc(main)
        .path()
        .resolve(serviceFile.path())
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
 * Compose the path to the main descriptor set file using the project Maven coordinates
 * as defined in the test project under `resources/annotator-plugin-test`.
 */
private fun mainDescriptorPath(): Path =
    DefaultJavaPaths.at(moduleDir)
        .buildRoot()
        .descriptors()
        .forSourceSet(SourceSet.MAIN_SOURCE_SET_NAME)
        .resolve("io.spine.test_${moduleDir.name}_3.14$DESC_EXTENSION")
