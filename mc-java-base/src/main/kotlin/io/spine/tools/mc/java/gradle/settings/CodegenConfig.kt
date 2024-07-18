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

@file:Suppress("TooManyFunctions")

package io.spine.tools.mc.java.gradle.settings

import com.google.common.collect.ImmutableList
import io.spine.annotation.Internal
import io.spine.base.CommandMessage
import io.spine.base.EventMessage
import io.spine.base.EventMessageField
import io.spine.base.MessageFile
import io.spine.base.RejectionMessage
import io.spine.base.UuidValue
import io.spine.protodata.FilePattern
import io.spine.protodata.FilePatternFactory
import io.spine.tools.java.code.Classpath
import io.spine.tools.java.code.UuidMethodFactory
import io.spine.tools.mc.java.settings.CodegenSettings
import io.spine.tools.mc.java.settings.GroupSettings
import io.spine.tools.mc.java.settings.MessageGroup
import io.spine.tools.mc.java.settings.Pattern
import io.spine.tools.mc.java.settings.SignalSettings
import io.spine.tools.mc.java.settings.TypePattern
import io.spine.tools.proto.code.ProtoTypeName
import java.util.function.Consumer
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

/**
 * A part of [McJavaOptions][io.spine.tools.mc.java.gradle.McJavaOptions] responsible
 * for code generation settings.
 */
public class CodegenConfig @Internal public constructor(private val project: Project) :
    Config<CodegenSettings>(project) {

    /**
     * Settings for the generated command code.
     */
    public val commands: SignalConfig = SignalConfig(project)

    /**
     * Settings for the generated event code.
     */
    public val events: SignalConfig = SignalConfig(project)

    /**
     * Settings for the generated rejection code.
     */
    public val rejections: SignalConfig = SignalConfig(project)

    /**
     * Settings for the generated entities code.
     */
    public val entities: EntityConfig = EntityConfig(project)

    /**
     * Settings for the generated code of [io.spine.base.UuidValue] types.
     */
    public val uuids: UuidConfig = UuidConfig(project)

    /**
     * Settings for the generated validation code.
     */
    public val validation: ValidationConfig = ValidationConfig(project)

    /**
     * Settings for the generated code of grouped messages.
     */
    public val messageGroups: MutableSet<MessageGroup> = HashSet()

    /**
     * Obtains the configuration settings for the generated validation code.
     */
    @Deprecated("Please use property syntax instead.", ReplaceWith("validation"))
    public fun validation(): ValidationConfig = validation

    /**
     * Configures code generation for command messages.
     */
    public fun forCommands(action: Action<SignalConfig>) {
        action.execute(commands)
    }

    /**
     * Configures code generation for event messages.
     *
     * Settings applied to events do not automatically apply to rejections as well.
     */
    public fun forEvents(action: Action<SignalConfig>) {
        action.execute(events)
    }

    /**
     * Configures code generation for rejection messages.
     *
     * Settings applied to events do not automatically apply to rejections as well.
     */
    public fun forRejections(action: Action<SignalConfig>) {
        action.execute(rejections)
    }

    /**
     * Configures code generation for entity state messages.
     */
    public fun forEntities(action: Action<EntityConfig>) {
        action.execute(entities)
    }

    /**
     * Configures code generation for UUID messages.
     */
    public fun forUuids(action: Action<UuidConfig>) {
        action.execute(uuids)
    }

    /**
     * Configures code generation for validation messages.
     */
    public fun validation(action: Action<ValidationConfig>) {
        action.execute(validation)
    }

    /**
     * Configures code generation for a group of messages.
     *
     * The group is defined by a file-based selector.
     *
     * @see by
     */
    public fun forMessages(filePattern: FilePattern, action: Action<MessageGroupConfig>) {
        val pattern = Pattern.newBuilder()
            .setFile(filePattern)
            .build()
        val config = MessageGroupConfig(project, pattern)
        action.execute(config)
        messageGroups.add(config.toProto())
    }

    /**
     * Obtains an instance of [FilePatternFactory] which creates file patterns.
     *
     * @see forMessages
     */
    public fun by(): FilePatternFactory {
        return FilePatternFactory
    }

    /**
     * Configures code generation for particular message.
     */
    public fun forMessage(protoTypeName: String, action: Action<MessageGroupConfig>) {
        val name = ProtoTypeName.newBuilder()
            .setValue(protoTypeName)
            .build()
        val pattern = Pattern.newBuilder()
            .setType(TypePattern.newBuilder().setExpectedType(name))
            .build()
        val config = MessageGroupConfig(project, pattern)
        action.execute(config)
        messageGroups.add(config.toProto())
    }

    override fun toProto(): CodegenSettings {
        val signalSettings = SignalSettings.newBuilder()
            .setCommands(commands.toProto())
            .setEvents(events.toProto())
            .setRejections(rejections.toProto())
            .build()

        val groupSettings = GroupSettings.newBuilder()
        messageGroups.forEach(Consumer { groupSettings.addGroup(it) })

        val classpath = buildClasspath()

        val builder = CodegenSettings.newBuilder()
            .setSignalSettings(signalSettings)
            .setGroupSettings(groupSettings)
            .setEntities(entities.toProto())
            .setValidation(validation.toProto())
            .setUuids(uuids.toProto())
            .setClasspath(classpath)
        return builder.build()
    }

    private fun buildClasspath(): Classpath {
        val classpath = Classpath.newBuilder()
        val javaCompileViews = project.tasks.withType(JavaCompile::class.java)
        ImmutableList.copyOf(javaCompileViews)
            .map { it.classpath }
            .map { it.files }
            .flatten()
            .map { it.absolutePath }
            .forEach { classpath.addItem(it) }
        return classpath.build()
    }
}

/**
 * Sets up default values for the [CodegenConfig] settings.
 *
 * Default values are based on the Gradle conventions principle.
 * If the user changes the value, the default is completely overridden,
 * even if the used invokes a method which has a name related to appending, e.g. `add()`.
 */
public fun CodegenConfig.applyConventions() {
    commands.convention(
        MessageFile.COMMANDS,
        CommandMessage::class.java
    )
    events.convention(
        MessageFile.EVENTS,
        EventMessage::class.java,
        EventMessageField::class.java
    )
    rejections.convention(
        MessageFile.REJECTIONS,
        RejectionMessage::class.java,
        EventMessageField::class.java
    )
    uuids.convention(
        UuidMethodFactory::class.java,
        UuidValue::class.java
    )
}
