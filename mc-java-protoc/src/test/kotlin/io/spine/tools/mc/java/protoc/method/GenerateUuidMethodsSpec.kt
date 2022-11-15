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
package io.spine.tools.mc.java.protoc.method

import com.google.common.truth.Truth.assertThat
import io.spine.testing.Assertions.assertIllegalArgument
import io.spine.testing.Assertions.assertNpe
import io.spine.tools.java.code.Classpath
import io.spine.tools.java.code.JavaClassName
import io.spine.tools.java.code.MethodFactory
import io.spine.tools.mc.java.codegen.MethodFactoryName
import io.spine.tools.mc.java.codegen.Uuids
import io.spine.tools.mc.java.protoc.ExternalClassLoader
import io.spine.tools.mc.java.protoc.given.TestMethodFactory
import io.spine.tools.protoc.plugin.method.NonEnhancedMessage
import io.spine.tools.protoc.plugin.method.TestUuidValue
import io.spine.type.MessageType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("`GenerateUuidMethods` should")
internal class GenerateUuidMethodsSpec {

    @Nested
    @DisplayName("throw `NullPointerException` if")
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    internal inner class ThrowNpe {

        @Test
        fun `is created with 'null' arguments` () {
            assertNpe { GenerateUuidMethods(null, MethodFactoryName.getDefaultInstance()) }
            assertNpe { GenerateUuidMethods(testClassLoader(), null) }
        }

        @Test
        fun `'null' 'MessageType' is supplied`() {
            val config = newTaskConfig("test")
            val task = newTask(config)
            assertNpe { task.generateFor(null) }
        }
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["", "  "])
    @DisplayName("")
    fun `throw 'IllegalArgumentException' if factory name is `(factoryName: String) {
        val config = newTaskConfig(factoryName)
        assertIllegalArgument { newTask(config) }
    }

    @Nested
    @DisplayName("generate empty result if")
    internal inner class GenerateEmptyResult {

        @Test
        fun `message is not UUID value`() =
            assertEmptyResult(TestMethodFactory::class.java.name)

        private fun assertEmptyResult(factoryName: String) {
            val config = newTaskConfig(factoryName)
            val result = newTask(config)
                .generateFor(MessageType(NonEnhancedMessage.getDescriptor()))
            assertThat(result).isEmpty()
        }
    }

    @Test
    fun `generate new methods`() {
        val config = newTaskConfig(TestMethodFactory::class.java.name)
        assertThat(newTask(config).generateFor(testType()))
            .isNotEmpty()
    }
}

private fun newTaskConfig(factoryName: String): Uuids {
    val factoryClass = JavaClassName.newBuilder()
        .setCanonical(factoryName)
        .buildPartial()
    val name = MethodFactoryName.newBuilder()
        .setClassName(factoryClass)
        .build()
    return Uuids.newBuilder()
        .addMethodFactory(name)
        .build()
}

private fun newTask(config: Uuids): GenerateUuidMethods =
    GenerateUuidMethods(testClassLoader(), config.getMethodFactory(0))

private fun testClassLoader(): ExternalClassLoader<MethodFactory> =
    ExternalClassLoader(Classpath.getDefaultInstance(), MethodFactory::class.java)

private fun testType(): MessageType =
    MessageType(TestUuidValue.getDescriptor())
