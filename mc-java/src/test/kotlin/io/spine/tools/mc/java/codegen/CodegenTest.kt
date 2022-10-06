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
package io.spine.tools.mc.java.codegen

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import io.spine.base.CommandMessage
import io.spine.base.EntityState
import io.spine.base.EventMessage
import io.spine.base.EventMessageField
import io.spine.base.MessageFile
import io.spine.base.MessageFile.COMMANDS
import io.spine.base.MessageFile.EVENTS
import io.spine.base.RejectionMessage
import io.spine.base.UuidValue
import io.spine.option.OptionsProto
import io.spine.query.EntityStateField
import io.spine.tools.java.code.UuidMethodFactory
import io.spine.tools.mc.java.applyStandard
import io.spine.tools.mc.java.gradle.McJavaOptions
import io.spine.tools.mc.java.gradle.codegen.CodegenOptionsConfig
import io.spine.tools.mc.java.gradle.codegen.SignalConfig
import io.spine.tools.mc.java.gradle.mcJava
import io.spine.tools.mc.java.gradle.plugins.McJavaPlugin
import io.spine.tools.proto.code.ProtoTypeName
import java.io.File
import java.nio.file.Path
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class `'codegen { }' block should` {

    private lateinit var options: McJavaOptions

    @BeforeEach
    fun prepareExtension(@TempDir projectDir: File) {
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()
        // Add repositories for resolving locally built artifacts (via `mavenLocal()`)
        // and their dependencies via `mavenCentral()`.
        project.repositories.applyStandard()
        project.apply {
            it.plugin("java")
            it.plugin("com.google.protobuf")
            it.plugin(McJavaPlugin::class.java)
        }
        options = project.mcJava
    }

    @Test
    fun `apply changes immediately`() {
        val factoryName = "fake.Factory"
        options.codegen { config ->
            config.forUuids {
                it.generateMethodsWith(factoryName)
            }
        }
        val config = options.codegen.toProto()
        assertThat(config.uuids.methodFactoryList)
            .hasSize(1)
        assertThat(
            config.uuids
                .methodFactoryList[0]
                .className
                .canonical
        ).isEqualTo(factoryName)
    }

    @Nested
    inner class `configure generation of` {

        @Test
        fun commands() {
            val firstInterface = "test.iface.Command"
            val secondInterface = "test.iface.TestCommand"
            val fieldSuperclass = "test.cmd.Field"
            val suffix = "_my_commands.proto"
            options.codegen { config: CodegenOptionsConfig ->
                config.forCommands { commands: SignalConfig ->
                    commands.includeFiles(commands.by().suffix(suffix))
                    commands.markAs(firstInterface)
                    commands.markAs(secondInterface)
                    commands.markFieldsAs(fieldSuperclass)
                }
            }
            val config = options.codegen.toProto()
            val commands = config.commands
            assertThat(commands.patternList)
                .hasSize(1)
            assertThat(commands.patternList[0].suffix)
                .isEqualTo(suffix)
            assertThat(commands.addInterfaceList.map { it.name.canonical })
                .containsExactly(firstInterface, secondInterface)
            assertThat(commands.generateFields.superclass.canonical)
                .isEqualTo(fieldSuperclass)
        }

        @Test
        fun events() {
            val iface = "test.iface.Event"
            val fieldSuperclass = "test.event.Field"
            val prefix = "my_"
            options.codegen { config: CodegenOptionsConfig ->
                config.forEvents { events: SignalConfig ->
                    events.includeFiles(events.by().prefix(prefix))
                    events.markAs(iface)
                    events.markFieldsAs(fieldSuperclass)
                }
            }
            val config = options.codegen.toProto()
            val events = config.events
            assertThat(events.patternList)
                .hasSize(1)
            assertThat(events.patternList[0].prefix)
                .isEqualTo(prefix)
            assertThat(events.addInterfaceList.map { it.name.canonical })
                .containsExactly(iface)
            assertThat(events.generateFields.superclass.canonical)
                .isEqualTo(fieldSuperclass)
        }

        @Test
        fun rejections() {
            val iface = "test.iface.RejectionMessage"
            val fieldSuperclass = "test.rejection.Field"
            val regex = ".*rejection.*"
            options.codegen { config: CodegenOptionsConfig ->
                config.forEvents { events: SignalConfig ->
                    events.includeFiles(events.by().regex(regex))
                    events.markAs(iface)
                    events.markFieldsAs(fieldSuperclass)
                }
            }
            val config = options.codegen.toProto()
            val events = config.events
            assertThat(events.patternList)
                .hasSize(1)
            assertThat(events.patternList[0].regex)
                .isEqualTo(regex)
            assertThat(events.addInterfaceList.map { it.name.canonical })
                .containsExactly(iface)
            assertThat(events.generateFields.superclass.canonical)
                .isEqualTo(fieldSuperclass)
        }

        @Test
        fun `rejections separately from events`() {
            val eventInterface = "test.iface.EventMsg"
            val rejectionInterface = "test.iface.RejectionMsg"
            options.codegen { config ->
                config.forEvents {
                    it.markAs(eventInterface)
                }
                config.forRejections {
                    it.markAs(rejectionInterface)
                }
            }
            val config = options.codegen.toProto()
            val eventInterfaces = config.events.addInterfaceList
            val rejectionInterfaces = config.rejections.addInterfaceList
            assertThat(eventInterfaces)
                .hasSize(1)
            assertThat(rejectionInterfaces)
                .hasSize(1)
            assertThat(eventInterfaces.first().name.canonical)
                .isEqualTo(eventInterface)
            assertThat(rejectionInterfaces.first().name.canonical)
                .isEqualTo(rejectionInterface)
        }

        @Test
        fun entities() {
            val iface = "custom.EntityMessage"
            val fieldSupertype = "custom.FieldSupertype"
            val suffix = "view.proto"
            val option = "view"
            options.codegen { config ->
                config.forEntities {
                    it.options.add(option)
                    it.includeFiles(it.by().suffix(suffix))
                    it.skipQueries()
                    it.markAs(iface)
                    it.markFieldsAs(fieldSupertype)
                }
            }
            val config = options.codegen.toProto().entities
            assertThat(config.addInterfaceList.map { it.name.canonical })
                .containsExactly(iface)
            assertThat(config.generateFields.superclass.canonical)
                .isEqualTo(fieldSupertype)
            assertThat(config.patternList)
                .hasSize(1)
            assertThat(config.patternList.first().suffix)
                .isEqualTo(suffix)
            assertThat(config.optionList)
                .hasSize(1)
            assertThat(config.optionList.first().name)
                .isEqualTo(option)
        }

        @Test
        fun `UUID messages`() {
            val iface = "custom.RandomizedId"
            val methodFactory = "custom.MethodFactory"
            options.codegen { config ->
                config.forUuids {
                    it.markAs(iface)
                    it.generateMethodsWith(methodFactory)
                }
            }
            val config = options.codegen.toProto().uuids
            assertThat(config.addInterfaceList.map { it.name.canonical })
                .containsExactly(iface)
            assertThat(config.methodFactoryList)
                .hasSize(1)
            assertThat(config.methodFactoryList.first().className.canonical)
                .isEqualTo(methodFactory)
        }

        @Test
        fun `arbitrary message groups`() {
            val firstInterface = "com.acme.Foo"
            val secondInterface = "com.acme.Bar"
            val methodFactory = "custom.MethodFactory"
            val classFactory = "custom.NestedClassFactory"
            val fieldSuperclass = "acme.Searchable"
            val firstMessageType = "acme.small.yellow.Bird"
            options.codegen { config ->
                config.forMessage(firstMessageType) {
                    it.markAs(firstInterface)
                    it.markFieldsAs(fieldSuperclass)
                    it.generateNestedClassesWith(classFactory)
                }
                config.forMessages(config.by().regex(".+_.+")) {
                    it.markAs(secondInterface)
                    it.generateMethodsWith(methodFactory)
                }
            }
            val configs = options.codegen.toProto().messagesList
            assertThat(configs)
                .hasSize(2)
            var (first, second) = configs

            // Restore ordering. When generating code, it does not matter which group goes
            // after which.
            if (second.pattern.hasType()) {
                val t = second
                second = first
                first = t
            }

            assertThat(first.pattern.type.expectedType.value)
                .isEqualTo(firstMessageType)
            assertThat(first.addInterfaceList.first().name.canonical)
                .isEqualTo(firstInterface)
            assertThat(first.generateFields.superclass.canonical)
                .isEqualTo(fieldSuperclass)
            assertThat(first.generateNestedClassesList)
                .hasSize(1)
            assertThat(first.generateNestedClassesList.first().factory.className.canonical)
                .isEqualTo(classFactory)

            assertThat(second.pattern.file.hasRegex())
                .isTrue()
            assertThat(second.addInterfaceList.first().name.canonical)
                .isEqualTo(secondInterface)
            assertThat(second.generateMethodsList.first().factory.className.canonical)
                .isEqualTo(methodFactory)
        }

        @Test
        fun validation() {
            options.codegen { config ->
                config.validation {
                    it.skipBuilders()
                    it.skipValidation()
                }
            }
            val validation = options.codegen.toProto().validation
            assertThat(validation.skipBuilders)
                .isTrue()
            assertThat(validation.skipValidation)
                .isTrue()
        }
    }

    @Nested
    inner class `provide reasonable defaults for` {

        @Test
        fun commands() {
            val config = options.codegen.toProto()
            val commands = config.commands
            assertThat(commands.patternList)
                .hasSize(1)
            assertThat(commands.patternList[0].suffix)
                .isEqualTo(COMMANDS.suffix())
            assertThat(commands.addInterfaceList.map { it.name.canonical })
                .containsExactly(CommandMessage::class.qualifiedName)
            assertThat(commands.generateFields)
                .isEqualTo(GenerateFields.getDefaultInstance())
        }

        @Test
        fun events() {
            val config = options.codegen.toProto()
            val events = config.events
            assertThat(events.patternList)
                .hasSize(1)
            assertThat(events.patternList[0].suffix)
                .isEqualTo(EVENTS.suffix())
            assertThat(events.addInterfaceList.map { it.name.canonical })
                .containsExactly(EventMessage::class.qualifiedName)
            assertThat(events.generateFields.superclass.canonical)
                .isEqualTo(EventMessageField::class.qualifiedName)
        }

        @Test
        fun rejections() {
            val config = options.codegen.toProto()
            val events = config.rejections
            assertThat(events.patternList)
                .hasSize(1)
            assertThat(events.patternList[0].suffix)
                .isEqualTo(MessageFile.REJECTIONS.suffix())
            assertThat(events.addInterfaceList.map { it.name.canonical })
                .containsExactly(RejectionMessage::class.qualifiedName)
            assertThat(events.generateFields.superclass.canonical)
                .isEqualTo(EventMessageField::class.qualifiedName)
        }

        @Test
        fun entities() {
            val config = options.codegen.toProto().entities
            assertThat(config.addInterfaceList.map { it.name.canonical })
                .containsExactly(EntityState::class.qualifiedName)
            assertThat(config.generateFields.superclass.canonical)
                .isEqualTo(EntityStateField::class.qualifiedName)
            assertThat(config.patternList)
                .isEmpty()
            assertThat(config.optionList)
                .hasSize(1)
            assertThat(config.optionList.first().name)
                .isEqualTo(OptionsProto.entity.descriptor.name)
        }

        @Test
        fun `UUID messages`() {
            val config = options.codegen.toProto().uuids
            assertThat(config.addInterfaceList.map { it.name.canonical })
                .containsExactly(UuidValue::class.qualifiedName)
            assertThat(config.methodFactoryList)
                .hasSize(1)
            assertThat(config.methodFactoryList.first().className.canonical)
                .isEqualTo(UuidMethodFactory::class.qualifiedName)
        }

        @Test
        fun `arbitrary message groups`() {
            val config = options.codegen.toProto()
            assertThat(config.messagesList)
                .isEmpty()

            val type = "test.Message"
            options.codegen {
                it.forMessage(type) { /* Do nothing. */ }
            }
            val updatedConfig = options.codegen.toProto()
            assertThat(updatedConfig.messagesList)
                .hasSize(1)
            val typeName = ProtoTypeName.newBuilder().setValue(type)
            val typePattern = TypePattern.newBuilder()
                .setExpectedType(typeName)
            val pattern = Pattern.newBuilder()
                .setType(typePattern)
            assertThat(updatedConfig.messagesList.first())
                .isEqualTo(
                    Messages.newBuilder()
                        .setPattern(pattern)
                        .buildPartial()
                )
        }

        @Test
        fun validation() {
            val validation = options.codegen.toProto().validation
            assertThat(validation.skipBuilders)
                .isFalse()
            assertThat(validation.skipValidation)
                .isFalse()
        }
    }

    @Nested
    inner class `allow configuring generation of queries` {

        @Test
        fun `having queries turned by default`() {
            assertFlag().isTrue()
        }

        @Test
        fun `turning generation of queries off`() {
            options.codegen.forEntities {
                it.skipQueries()
            }
            assertFlag().isFalse()
        }

        @Test
        fun `turning generation of queries on`() {
            // Turn `off`, assuming that the default is `on`.
            options.codegen.forEntities {
                it.skipQueries()
            }

            // Turn `on`.
            options.codegen.forEntities {
                it.generateQueries()
            }

            assertFlag().isTrue()
        }

        private fun assertFlag() = assertThat(options.codegen.toProto().entities.generateQueries)
    }
}
