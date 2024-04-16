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
package io.spine.tools.mc.java.settings

import com.google.common.truth.Truth.assertThat
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
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
import io.spine.tools.mc.java.gradle.settings.MessageCodegenOptions
import io.spine.tools.mc.java.gradle.settings.SignalConfig
import io.spine.tools.mc.java.gradle.mcJava
import io.spine.tools.mc.java.gradle.plugins.McJavaPlugin
import io.spine.tools.proto.code.ProtoTypeName
import java.io.File
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir

@DisplayName("`codegen { }` block should`")
class CodegenBlockSpec {

    private lateinit var options: McJavaOptions
    private lateinit var projectDir: File

    /**
     * Creates the project in the given directory.
     *
     * The directory is set not to be cleaned up by JUnit because cleanup sometime
     * fails under Windows.
     * See [this comment](https://github.com/gradle/gradle/issues/12535#issuecomment-1064926489)
     * on the corresponding issue for details:
     *
     * The [projectDir] is set to be removed in the [removeTempDir] method.
     *
     * @see removeTempDir
     */
    @BeforeEach
    fun prepareExtension(
        @TempDir(cleanup = CleanupMode.NEVER) projectDir: File) {
        this.projectDir = projectDir
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

    @AfterEach
    fun removeTempDir() {
        projectDir.deleteOnExit()
    }

    @Test
    fun `apply changes immediately`() {
        val factoryName = "fake.Factory"
        options.codegen { config ->
            config.forUuids {
                it.generateMethodsWith(factoryName)
            }
        }
        val config = options.codegen!!.toProto()
        config.uuids.methodFactoryList shouldHaveSize 1
        config.uuids
            .methodFactoryList[0]
            .className
            .canonical shouldBe factoryName
    }

    @Nested
    @DisplayName("configure generation of")
    inner class ConfigureGeneration {

        @Test
        fun commands() {
            val firstInterface = "test.iface.Command"
            val secondInterface = "test.iface.TestCommand"
            val fieldSuperclass = "test.cmd.Field"
            val suffix = "_my_commands.proto"
            options.codegen { config: MessageCodegenOptions ->
                config.forCommands { commands: SignalConfig ->
                    with(commands) {
                        includeFiles(by().suffix(suffix))
                        markAs(firstInterface)
                        markAs(secondInterface)
                        markFieldsAs(fieldSuperclass)
                    }
                }
            }
            val config = options.codegen!!.toProto()

            config.commands.run {
                patternList shouldHaveSize 1
                patternList[0].suffix shouldBe suffix
                addInterfaceList.map { it.name.canonical } shouldContainExactly
                        listOf(firstInterface, secondInterface)
                generateFields.superclass.canonical shouldBe fieldSuperclass
            }
        }

        @Test
        fun events() {
            val iface = "test.iface.Event"
            val fieldSuperclass = "test.event.Field"
            val prefix = "my_"
            options.codegen { config: MessageCodegenOptions ->
                config.forEvents { events: SignalConfig ->
                    with(events) {
                        includeFiles(by().prefix(prefix))
                        markAs(iface)
                        markFieldsAs(fieldSuperclass)
                    }
                }
            }
            val config = options.codegen!!.toProto()

            config.events.run {
                patternList shouldHaveSize 1
                patternList[0].prefix shouldBe prefix
                addInterfaceList.map { it.name.canonical } shouldContainExactly listOf(iface)
                generateFields.superclass.canonical shouldBe fieldSuperclass
            }
        }

        @Test
        fun rejections() {
            val iface = "test.iface.RejectionMessage"
            val fieldSuperclass = "test.rejection.Field"
            val regex = ".*rejection.*"
            options.codegen { config: MessageCodegenOptions ->
                config.forEvents { events: SignalConfig ->
                    events.includeFiles(events.by().regex(regex))
                    events.markAs(iface)
                    events.markFieldsAs(fieldSuperclass)
                }
            }
            val config = options.codegen!!.toProto()

            config.events.run {
                patternList shouldHaveSize 1
                patternList[0].regex shouldBe regex
                addInterfaceList.map { it.name.canonical } shouldContainExactly listOf(iface)
                generateFields.superclass.canonical shouldBe fieldSuperclass
            }
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
            val config = options.codegen!!.toProto()
            val eventInterfaces = config.events.addInterfaceList
            val rejectionInterfaces = config.rejections.addInterfaceList

            eventInterfaces shouldHaveSize 1
            rejectionInterfaces shouldHaveSize 1
            eventInterfaces.first().name.canonical shouldBe eventInterface
            rejectionInterfaces.first().name.canonical shouldBe rejectionInterface
        }

        @Test
        fun entities() {
            val iface = "custom.EntityMessage"
            val fieldSupertype = "custom.FieldSupertype"
            val option = "view"
            options.codegen { config ->
                config.forEntities {
                    it.options.add(option)
                    it.skipQueries()
                    it.markAs(iface)
                    it.markFieldsAs(fieldSupertype)
                }
            }
            val entities = options.codegen!!.toProto().entities

            entities.run {
                addInterfaceList.map { it.name.canonical } shouldContainExactly listOf(iface)
                generateFields.superclass.canonical shouldBe fieldSupertype
                optionList shouldHaveSize 1
                optionList.first().name shouldBe option
            }
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
            val uuids = options.codegen!!.toProto().uuids
            uuids.run {
                addInterfaceList.map { it.name.canonical } shouldContainExactly listOf(iface)
                methodFactoryList shouldHaveSize 1
                methodFactoryList.first().className.canonical shouldBe methodFactory
            }
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
            val configs = options.codegen!!.toProto().messagesList

            configs shouldHaveSize 2

            var (first, second) = configs

            // Restore ordering. When generating code, it does not matter which group goes
            // after which.
            if (second.pattern.hasType()) {
                val t = second
                second = first
                first = t
            }

            first.run {
                pattern.type.expectedType.value shouldBe firstMessageType
                addInterfaceList.first().name.canonical shouldBe firstInterface
                generateFields.superclass.canonical shouldBe fieldSuperclass
                generateNestedClassesList shouldHaveSize 1
                generateNestedClassesList.first().factory.className.canonical shouldBe classFactory
            }

            second.run {
                pattern.file.hasRegex() shouldBe true
                addInterfaceList.first().name.canonical shouldBe secondInterface
                generateMethodsList.first().factory.className.canonical shouldBe methodFactory
            }
        }

    }

    @Nested
    @DisplayName("provide reasonable defaults for")
    inner class ProvideDefaults {

        @Test
        fun commands() {
            val config = options.codegen!!.toProto()
            val commands = config.commands

            commands.run {
                patternList shouldHaveSize 1
                patternList[0].suffix shouldBe COMMANDS.suffix()
                addInterfaceList.map { it.name.canonical } shouldContainExactly
                        listOf(CommandMessage::class.qualifiedName)
                generateFields shouldBe GenerateFields.getDefaultInstance()
            }
        }

        @Test
        fun events() {
            val config = options.codegen!!.toProto()
            val events = config.events

            events.run {
                patternList shouldHaveSize 1
                patternList[0].suffix shouldBe EVENTS.suffix()
                addInterfaceList.map { it.name.canonical } shouldContainExactly
                        listOf(EventMessage::class.qualifiedName)
                generateFields.superclass.canonical shouldBe
                        EventMessageField::class.qualifiedName
            }
        }

        @Test
        fun rejections() {
            val config = options.codegen!!.toProto()
            val events = config.rejections

            events.run {
                patternList shouldHaveSize 1
                patternList[0].suffix shouldBe MessageFile.REJECTIONS.suffix()
                addInterfaceList.map { it.name.canonical } shouldContainExactly
                        listOf(RejectionMessage::class.qualifiedName)
                generateFields.superclass.canonical shouldBe
                        EventMessageField::class.qualifiedName
            }
        }

        @Test
        fun entities() {
            val entities = options.codegen!!.toProto().entities

            entities.run {
                addInterfaceList.map { it.name.canonical } shouldContainExactly
                        listOf(EntityState::class.qualifiedName)
                generateFields.superclass.canonical shouldBe
                        EntityStateField::class.qualifiedName
                optionList shouldHaveSize 1
                optionList.first().name shouldBe OptionsProto.entity.descriptor.name
            }
        }

        @Test
        fun `UUID messages`() {
            val uuids = options.codegen!!.toProto().uuids

            uuids.run {
                addInterfaceList.map { it.name.canonical } shouldContainExactly
                        listOf(UuidValue::class.qualifiedName)
                methodFactoryList shouldHaveSize 1
                methodFactoryList.first().className.canonical shouldBe
                        UuidMethodFactory::class.qualifiedName
            }
        }

        @Test
        fun `arbitrary message groups`() {
            val config = options.codegen!!.toProto()

            config.messagesList shouldBe emptyList()

            val type = "test.Message"
            options.codegen {
                it.forMessage(type) { /* Do nothing. */ }
            }
            val updatedConfig = options.codegen!!.toProto()

            updatedConfig.messagesList shouldHaveSize 1
            val typeName = ProtoTypeName.newBuilder().setValue(type)
            val typePattern = TypePattern.newBuilder()
                .setExpectedType(typeName)
            val pattern = Pattern.newBuilder()
                .setType(typePattern)

            updatedConfig.messagesList.first() shouldBe
                    Messages.newBuilder()
                        .setPattern(pattern)
                        .buildPartial()
        }

        @Test
        fun validation() {
            val validation = options.codegen!!.toProto().validation
            validation.run {
                version.shouldBeEmpty()
            }
        }
    }

    @Nested
    @DisplayName("allow configuring generation of queries")
    inner class AllowConfiguring {

        @Test
        fun `having queries turned by default`() {
            assertFlag().isTrue()
        }

        @Test
        fun `turning generation of queries off`() {
            options.codegen!!.forEntities {
                it.skipQueries()
            }
            assertFlag().isFalse()
        }

        @Test
        fun `turning generation of queries on`() {
            // Turn `off`, assuming that the default is `on`.
            options.codegen!!.forEntities {
                it.skipQueries()
            }

            // Turn `on`.
            options.codegen!!.forEntities {
                it.generateQueries()
            }

            assertFlag().isTrue()
        }

        private fun assertFlag() = assertThat(options.codegen!!.toProto().entities.generateQueries)
    }
}
