/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.option.OptionsProto
import io.spine.query.EntityStateField
import io.spine.tools.mc.java.applyStandard
import io.spine.tools.mc.java.gradle.McJavaOptions
import io.spine.tools.mc.java.gradle.mcJava
import io.spine.tools.mc.java.gradle.plugins.McJavaPlugin
import io.spine.tools.mc.java.gradle.settings.UuidSettings.DEFAULT_ACTION
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
     * Calculates the [SignalSettings] after [options] are modified by a test body.
     */
    private val signalSettings: SignalSettings
        get() = options.codegen!!.toProto().signalSettings

    /**
     * Creates the project in the given directory.
     *
     * The directory is set not to be cleaned up by JUnit because cleanup sometimes
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
        val actionName = "fake.Action"
        options.codegen { settings ->
            settings.forUuids {
                it.useAction(actionName)
            }
        }
        val settings = options.codegen!!.toProto()
        settings.uuids.methodFactoryList shouldHaveSize 1
        settings.uuids
            .methodFactoryList[0]
            .className
            .canonical shouldBe actionName
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
            options.codegen { settings ->
                settings.forCommands { commands ->
                    with(commands) {
                        includeFiles(by().suffix(suffix))
                        markAs(firstInterface)
                        markAs(secondInterface)
                        markFieldsAs(fieldSuperclass)
                    }
                }
            }

            signalSettings.commands.run {
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
            options.codegen { settings ->
                settings.forEvents { events ->
                    with(events) {
                        includeFiles(by().prefix(prefix))
                        markAs(iface)
                        markFieldsAs(fieldSuperclass)
                    }
                }
            }

            signalSettings.events.run {
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
            options.codegen { settings ->
                settings.forEvents { events ->
                    events.includeFiles(events.by().regex(regex))
                    events.markAs(iface)
                    events.markFieldsAs(fieldSuperclass)
                }
            }

            signalSettings.events.run {
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
            options.codegen { settings ->
                settings.forEvents {
                    it.markAs(eventInterface)
                }
                settings.forRejections {
                    it.markAs(rejectionInterface)
                }
            }

            val eventInterfaces = signalSettings.events.addInterfaceList
            val rejectionInterfaces = signalSettings.rejections.addInterfaceList

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
            options.codegen { settings ->
                settings.forEntities {
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
            val customAction = "custom.UuidCodegenAction"
            options.codegen { settings ->
                settings.forUuids {
                    it.markAs(iface)
                    it.useAction(customAction)
                }
            }
            val uuids = options.codegen!!.toProto().uuids
            uuids.run {
                actionList shouldHaveSize 1
                actionList.first() shouldBe customAction
            }
        }

        @Test
        fun `arbitrary message groups`() {
            val firstInterface = "com.acme.Foo"
            val secondInterface = "com.acme.Bar"
            val methodFactory = "custom.MethodFactory"
            val nestedClassAction = "custom.NestedClassAction"
            val anotherNestedClassAction = "custom.AnotherNestedClassAction"
            val fieldSuperclass = "acme.Searchable"
            val firstMessageType = "acme.small.yellow.Bird"
            options.codegen { settings ->
                settings.forMessage(firstMessageType) {
                    it.markAs(firstInterface)
                    it.markFieldsAs(fieldSuperclass)
                    it.useAction(nestedClassAction)
                }
                settings.forMessages(settings.by().regex(".+_.+")) {
                    it.markAs(secondInterface)
                    it.generateMethodsWith(methodFactory)
                    it.useAction(anotherNestedClassAction)
                }
            }
            val groups = options.codegen!!.toProto().groupSettings.groupList

            groups shouldHaveSize 2

            var (first, second) = groups

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
                actionList shouldHaveSize 1
                actionList.first() shouldBe nestedClassAction
            }

            second.run {
                pattern.file.hasRegex() shouldBe true
                addInterfaceList.first().name.canonical shouldBe secondInterface
                generateMethodsList.first().factory.className.canonical shouldBe methodFactory
                actionList.first() shouldBe anotherNestedClassAction
            }
        }

    }

    @Nested
    @DisplayName("provide reasonable defaults for")
    inner class ProvideDefaults {

        @Test
        fun commands() {
            signalSettings.commands.run {
                patternList shouldHaveSize 1
                patternList[0].suffix shouldBe COMMANDS.suffix()
                addInterfaceList.map { it.name.canonical } shouldContainExactly
                        listOf(CommandMessage::class.qualifiedName)
                generateFields shouldBe GenerateFields.getDefaultInstance()
            }
        }

        @Test
        fun events() {
            signalSettings.events.run {
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
            signalSettings.rejections.run {
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
                actionList shouldHaveSize 1
                actionList.first() shouldBe DEFAULT_ACTION
            }
        }

        @Test
        fun `arbitrary message groups`() {
            val settings = options.codegen!!.toProto()

            settings.groupSettings.groupList shouldBe emptyList()

            val type = "test.Message"
            options.codegen {
                it.forMessage(type) { /* Do nothing. */ }
            }
            val updated = options.codegen!!.toProto()

            updated.groupSettings.groupList shouldHaveSize 1
            val typeName = ProtoTypeName.newBuilder().setValue(type)
            val typePattern = TypePattern.newBuilder()
                .setExpectedType(typeName)
            val pattern = Pattern.newBuilder()
                .setType(typePattern)

            updated.groupSettings.groupList.first() shouldBe
                    MessageGroup.newBuilder()
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
