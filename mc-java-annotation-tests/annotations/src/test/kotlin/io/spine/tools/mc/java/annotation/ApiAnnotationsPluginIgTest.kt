/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.annotation

import com.google.protobuf.Descriptors.FileDescriptor
import given.annotation.InternalAllMultiple
import given.annotation.InternalField
import given.annotation.InternalMessage.InternalOne
import given.annotation.InternalMessage.InternalTwo
import given.annotation.NoInternalOptions
import given.annotation.NoInternalOptions.FirstToCome
import given.annotation.NoInternalOptionsMultiple
import given.annotation.OuterInternal
import given.annotation.ReveringFileOption
import given.annotation.ServiceToGoGrpc
import given.annotation.SpiAll
import given.annotation.SpiServiceOuterClass
import io.spine.annotation.Internal
import io.spine.annotation.SPI
import io.spine.code.proto.FileName
import io.spine.code.proto.FileSet
import io.spine.code.proto.TypeSet
import io.spine.tools.div
import io.spine.tools.fs.DirectoryName
import io.spine.tools.java.fs.SourceFile
import io.spine.tools.mc.java.annotation.assertions.assertAnnotated
import io.spine.tools.mc.java.annotation.assertions.assertAnnotationOfAccessors
import io.spine.tools.mc.java.annotation.assertions.assertNotAnnotated
import io.spine.tools.mc.java.annotation.check.FieldAnnotationCheck
import io.spine.tools.mc.java.annotation.check.NestedTypeFieldsAnnotationCheck
import io.spine.tools.mc.java.annotation.check.NestedTypesAnnotationCheck
import io.spine.tools.mc.java.annotation.check.SourceCheck
import io.spine.tools.mc.java.annotation.check.TypeAnnotationCheck
import java.nio.file.Path
import kotlin.io.path.div
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.impl.AbstractJavaSource
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests annotating generated Java code with API level annotations, such as
 * [Internal] or [SPI].
 *
 * The subject of test is [io.spine.tools.mc.annotation.ApiAnnotationsPlugin] which is
 * a plugin to ProtoData.
 *
 * We test the plugin as a part of the Gradle build performed by McJava Gradle plugin executed
 * when processing Protobuf files declared in `testFixtures/proto` of this module.
 */
@DisplayName("`ApiAnnotationsPlugin` should")
internal class ApiAnnotationsPluginIgTest {

    @Nested
    inner class
    `annotate with '@Internal' when '(internal_all) = true'` {

        @Nested
        inner class
        `and 'java_multiple_files = false'` {

            @Test
            fun `an outer class of generated messages`() =
                assertAnnotated(OuterInternal::class.java, Internal::class.java)
        }

        @Nested
        inner class
        `and 'java_multiple_files = true'` {

            @Test
            fun `top level message classes`() {
                InternalAllMultiple.getDescriptor().messageClasses().forEach {
                    assertAnnotated(it, Internal::class.java)
                }
            }
        }
    }

    @Nested
    inner class
    `annotate with '@Internal'` {

        @Nested
        inner class
        `when 'java_multiple_files = false'` {

            @Test
            fun `a nested class of a message type marked '(internal_type) = true`() {
                assertAnnotated(InternalOne::class.java, Internal::class.java)
                assertAnnotated(InternalTwo::class.java, Internal::class.java)
            }

            @Test
            fun `accessors for fields with '(internal) = true'`() {
                assertAnnotationOfAccessors(
                    InternalField.EnclosedWithInternalField::class.java,
                    "value",
                    Internal::class.java,
                    true
                )
            }
        }

        @Nested
        inner class
        `when 'java_multiple_files = true'` {

            @Test
            fun `accessors for fields with '(internal) = true'`() =
                assertAnnotationOfAccessors(
                    InternalField.EnclosedWithInternalField::class.java,
                    "value",
                    Internal::class.java,
                    true
                )
        }
    }

    @Nested
    inner class
    `annotate with '@SPI'` {

        @Test
        fun `gRPC services if service option is true`() {
            SpiServiceOuterClass.getDescriptor().serviceClasses().forEach {
                assertAnnotated(it, SPI::class.java)
            }
        }

        @Test
        fun `gRPC services if 'SPI_all = true'`() {
            SpiAll.getDescriptor().serviceClasses().forEach {
                assertAnnotated(it, SPI::class.java)
            }
        }

        @Test
        fun `message class when 'SPI_all = true'`() {
            SpiAll.getDescriptor().messageClasses().forEach {
                assertAnnotated(it, SPI::class.java)
            }
        }
    }

    @Nested
    internal inner class
    `not annotate` {

        @Test
        fun `if file option if false`() {
            assertNotAnnotated(FirstToCome::class.java, Internal::class.java)
        }

        @Test
        fun `service if file option is false`() {
            assertNotAnnotated(ServiceToGoGrpc::class.java, Internal::class.java)
        }

        @Test
        fun `multiple files if file option is false`() {
            NoInternalOptionsMultiple.getDescriptor().allJavaClasses().forEach {
                assertNotAnnotated(it, Internal::class.java)
            }
        }

        @Test
        fun `if message option is false`() {
            assertNotAnnotated(FirstToCome::class.java, Internal::class.java)
        }

        @Test
        fun `multiple files if message option is false`() {
            NoInternalOptionsMultiple.getDescriptor().allJavaClasses().forEach {
                assertNotAnnotated(it, Internal::class.java)
            }
        }

        @Test
        fun `accessors if field option is false`() {
            assertAnnotationOfAccessors(
                FirstToCome::class.java,
                "value",
                Internal::class.java,
                false
            )
        }

        @Test
        fun `accessors in multiple files if field option is false`() {
            NoInternalOptionsMultiple.getDescriptor().messageClasses().forEach {
                assertAnnotationOfAccessors(
                    it,
                    "value",
                    Internal::class.java,
                    false
                )
            }
        }

        @Test
        fun `gRPC services if service option is false`() {
            NoInternalOptions.getDescriptor().serviceClasses().forEach {
                assertNotAnnotated(it, Internal::class.java)
            }
        }

        @Test
        fun `if message option overrides file option`() {
            assertNotAnnotated(ReveringFileOption::class.java, Internal::class.java)
        }
    }
}

private fun FileDescriptor.typeSet(): TypeSet =
    TypeSet.from(this)

private fun FileDescriptor.allJavaClasses(): List<Class<out Any>> =
    typeSet().allTypes().map { it.javaClass() }

private fun FileDescriptor.messageClasses(): List<Class<out Any>> =
    typeSet().messageTypes().map { it.javaClass() }

private fun FileDescriptor.serviceClasses(): List<Class<out Any>> =
    typeSet().serviceTypes().map { it.javaClass() }
