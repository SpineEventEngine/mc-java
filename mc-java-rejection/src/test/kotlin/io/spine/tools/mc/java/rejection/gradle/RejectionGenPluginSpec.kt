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

import com.google.common.base.Preconditions
import io.spine.testing.TempDir
import io.spine.tools.code.SourceSetName
import io.spine.tools.code.SourceSetName.Companion.main
import io.spine.tools.code.SourceSetName.Companion.test
import io.spine.tools.gradle.task.JavaTaskName
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.gradle.testing.GradleProject.Companion.setupAt
import io.spine.tools.java.fs.DefaultJavaPaths
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.checkerframework.checker.nullness.qual.MonotonicNonNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`RejectionGenPlugin` should")
internal class RejectionGenPluginSpec {

    companion object {

        private lateinit var moduleDir: File

        @BeforeAll
        @JvmStatic
        fun generateRejections() {
            val projectDir = TempDir.forClass(RejectionGenPluginSpec::class.java)
            val project: GradleProject = setupAt(projectDir)
                .fromResources("rejections-gen-plugin-test")
                .copyBuildSrc()
                .create()
            moduleDir = projectDir.toPath()
                .resolve("tests")
                .toFile()
            // Executing the `compileTestJava` task should generate rejection types from both
            // `test` and `main` source sets.
            project.executeTask(JavaTaskName.compileTestJava)
        }
    }

    private fun generatedRoot(sourceSetName: SourceSetName): Path =
        DefaultJavaPaths.at(moduleDir)
            .generatedProto()
            .spine(sourceSetName)
            .path()

    private fun targetMainDir(): Path = generatedRoot(main)

    private fun targetTestDir(): Path = generatedRoot(test)

    private fun assertExists(path: Path) =
        assertTrue(Files.exists(path)) { "The path `$path` is expected to exist." }

    private fun assertJavaFileExists(packageDir: Path, typeName: String) {
        val file = packageDir.resolve("$typeName.java")
        assertExists(file)
    }

    @Nested
    internal inner class `place generated code under the 'spine' directory for` {

        @Test
        fun `'main' source set`() {
            assertExists(targetMainDir())
        }

        @Test
        fun `'test' source set`() {
            assertExists(targetTestDir())
        }
    }

    @Nested
    internal inner class `use the package specified in proto file options` {

        @Test
        fun `for 'main' source set`() {
            // As defined in `resources/.../main_rejections.proto`.
            val packageDir = targetMainDir().resolve("io/spine/sample/rejections")
            assertExists(packageDir)

            // As defined in `resources/.../main_rejections.proto`.
            assertJavaFileExists(packageDir, "Rejection1")
            assertJavaFileExists(packageDir, "Rejection2")
            assertJavaFileExists(packageDir, "Rejection3")
            assertJavaFileExists(packageDir, "Rejection4")
            assertJavaFileExists(packageDir, "RejectionWithRepeatedField")
            assertJavaFileExists(packageDir, "RejectionWithMapField")
        }

        @Test
        fun `for 'test' source set`() {
            // As defined in `resources/.../test_rejections.proto`.
            val packageDir = targetTestDir().resolve("io/spine/sample/rejections")
            assertExists(packageDir)

            // As defined in `resources/.../test_rejections.proto`.
            assertJavaFileExists(packageDir, "TestRejection1")
            assertJavaFileExists(packageDir, "TestRejection2")
        }
    }
}
