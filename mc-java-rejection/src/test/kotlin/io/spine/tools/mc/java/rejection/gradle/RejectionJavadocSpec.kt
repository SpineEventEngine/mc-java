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

import com.google.common.truth.Truth.assertThat
import io.spine.code.java.SimpleClassName
import io.spine.testing.TempDir
import io.spine.tools.code.SourceSetName.Companion.main
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.gradle.testing.GradleProject.Companion.setupAt
import io.spine.tools.java.code.BuilderSpec
import io.spine.tools.mc.java.gradle.McJavaTaskName.Companion.generateRejections
import io.spine.tools.mc.java.rejection.gen.RThrowableBuilderSpec.NEW_BUILDER_METHOD
import io.spine.tools.mc.java.rejection.gradle.TestEnv.expectedBuilderClassComment
import io.spine.tools.mc.java.rejection.gradle.TestEnv.expectedClassComment
import io.spine.tools.mc.java.rejection.gradle.TestEnv.expectedFirstFieldComment
import io.spine.tools.mc.java.rejection.gradle.TestEnv.expectedSecondFieldComment
import io.spine.tools.mc.java.rejection.gradle.TestEnv.rejectionWithJavadoc
import io.spine.tools.mc.java.rejection.gradle.TestEnv.rejectionsJavadocThrowableSource
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaDocCapableSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Rejection code generator for Javadoc should")
internal class RejectionJavadocSpec {

    companion object {
        private lateinit var generatedSource: JavaClassSource

        @BeforeAll
        @JvmStatic
        fun generateSources() {
            val projectDir = TempDir.forClass(RejectionJavadocSpec::class.java)
            val project: GradleProject = setupAt(projectDir)
                .copyBuildSrc()
                .fromResources("rejection-javadoc-test") // Contains `build.gradle.kts`
                .addFile("src/main/proto/javadoc_rejections.proto", rejectionWithJavadoc())
                .create()
            project.executeTask(generateRejections(main))
            val generatedFile = rejectionsJavadocThrowableSource(projectDir.toPath()).toFile()
            generatedSource = Roaster.parse(
                JavaClassSource::class.java, generatedFile
            )
        }
    }

    private fun assertRejectionJavadoc(rejection: JavaClassSource) {
        assertDoc(expectedClassComment(), rejection)
        assertMethodDoc(
            "@return a new builder for the rejection", rejection,
            NEW_BUILDER_METHOD
        )
    }

    private fun assertBuilderJavadoc(builder: JavaClassSource) {
        assertDoc(expectedBuilderClassComment(), builder)
        assertMethodDoc(
            "Creates the rejection from the builder and validates it.", builder,
            BuilderSpec.BUILD_METHOD_NAME
        )
        assertMethodDoc(expectedFirstFieldComment(), builder, "setId")
        assertMethodDoc(expectedSecondFieldComment(), builder, "setRejectionMessage")
    }

    private fun assertMethodDoc(
        expectedComment: String,
        source: JavaClassSource,
        methodName: String
    ) {
        val method = findMethod(source, methodName)
        assertDoc(expectedComment, method)
    }

    private fun findMethod(
        source: JavaClassSource,
        methodName: String
    ): MethodSource<JavaClassSource> {
        return source.methods.stream()
            .filter { m -> methodName == m.name }
            .findFirst()
            .orElseThrow { error("Cannot find the method `$methodName`.") }
    }

    private fun assertDoc(expectedText: String, source: JavaDocCapableSource<*>) {
        val javadoc = source.javaDoc
        assertThat(javadoc.fullText)
            .isEqualTo(expectedText)
    }

    @Nested
    internal inner class `generate Javadoc for` {

        @Test
        fun `rejection type`() {
            assertRejectionJavadoc(generatedSource)
        }

        @Test
        fun `'Builder' of rejection`() {
            val builderTypeName = SimpleClassName.ofBuilder().value()
            val builderType = generatedSource.getNestedType(builderTypeName)
            assertBuilderJavadoc(builderType as JavaClassSource)
        }
    }
}
