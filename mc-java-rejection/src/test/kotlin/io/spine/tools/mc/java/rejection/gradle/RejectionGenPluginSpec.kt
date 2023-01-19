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

import io.kotest.matchers.shouldBe
import io.spine.testing.TempDir
import io.spine.tools.code.SourceSetName
import io.spine.tools.code.SourceSetName.Companion.main
import io.spine.tools.code.SourceSetName.Companion.test
import io.spine.tools.fs.DirectoryName.generated
import io.spine.tools.fs.DirectoryName.spine
import io.spine.tools.gradle.task.JavaTaskName
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.gradle.testing.GradleProject.Companion.setupAt
import io.spine.tools.resolve
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
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
                .enableRunnerDebug()
                .create()
            moduleDir = projectDir.toPath()
                .resolve("sub-module")
                .toFile()
            // Executing the `compileTestJava` task should generate rejection types from both
            // `test` and `main` source sets.
            project.executeTask(JavaTaskName.compileTestJava)
        }
    }

    private fun generatedRoot(sourceSetName: SourceSetName): Path =
        moduleDir.toPath().resolve(generated).resolve(sourceSetName.value)

    private fun targetMainDir(): Path = generatedRoot(main)

    private fun targetTestDir(): Path = generatedRoot(test)

    private fun assertExists(path: Path) =
        assertTrue(Files.exists(path)) { "The path `$path` is expected to exist." }

    private fun assertJavaFileExists(packageDir: Path, typeName: String) {
        val file = packageDir.resolve("$typeName.java")
        assertExists(file)
    }

    @Nested
    @DisplayName("place generated code under the `spine` directory for")
    internal inner class SourceSetDirs {

        @Test
        fun `'main' source set`() {
            val mainSpine = targetMainDir().resolve(spine)
            assertExists(mainSpine)
            mainSpine.isDirectory() shouldBe true
            mainSpine.containsJavaFiles() shouldBe true
        }

        @Test
        fun `'test' source set`() {
            val testSpine = targetTestDir().resolve(spine)
            assertExists(testSpine)
            testSpine.isDirectory() shouldBe true
            testSpine.containsJavaFiles() shouldBe true
        }

        @Test
        fun `'testFixtures' source set`() {
            val testFixturesSpine = generatedRoot(SourceSetName("testFixtures")).resolve(spine)
            assertExists(testFixturesSpine)
            testFixturesSpine.isDirectory() shouldBe true
            testFixturesSpine.containsJavaFiles() shouldBe true
        }
    }

    @Nested
    @DisplayName("use the package specified in proto file options")
    internal inner class PackageName {

        @Test
        fun `for 'main' source set`() {
            // As defined in `resources/.../main_rejections.proto`.
            val packageDir = targetMainDir().resolve(spine).resolve("io/spine/sample/rejections")
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
            val packageDir = targetTestDir().resolve(spine).resolve("io/spine/sample/rejections")
            assertExists(packageDir)

            // As defined in `resources/.../test_rejections.proto`.
            assertJavaFileExists(packageDir, "TestRejection1")
            assertJavaFileExists(packageDir, "TestRejection2")
        }
    }
}

private fun Path.containsJavaFiles(): Boolean {
    val found = toFile().walk().find { file -> file.name.endsWith(".java") }
    return found != null
}
