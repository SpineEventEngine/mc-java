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

package io.spine.tools.mc.java.gradle.settings;

import com.google.common.collect.ImmutableList;
import io.spine.annotation.Internal;
import io.spine.base.CommandMessage;
import io.spine.base.EntityState;
import io.spine.base.EventMessage;
import io.spine.base.EventMessageField;
import io.spine.base.RejectionMessage;
import io.spine.base.UuidValue;
import io.spine.option.OptionsProto;
import io.spine.protodata.FilePatternFactory;
import io.spine.query.EntityStateField;
import io.spine.tools.java.code.Classpath;
import io.spine.tools.java.code.UuidMethodFactory;
import io.spine.tools.mc.java.settings.CodegenSettings;
import io.spine.tools.mc.java.settings.GroupSettings;
import io.spine.tools.mc.java.settings.MessageGroup;
import io.spine.tools.mc.java.settings.Pattern;
import io.spine.tools.mc.java.settings.SignalSettings;
import io.spine.tools.mc.java.settings.TypePattern;
import io.spine.tools.proto.code.ProtoTypeName;
import io.spine.protodata.FilePattern;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.MessageFile.COMMANDS;
import static io.spine.base.MessageFile.EVENTS;
import static io.spine.base.MessageFile.REJECTIONS;

/**
 * A part of {@link io.spine.tools.mc.java.gradle.McJavaOptions McJavaOptions} responsible
 * for code generation settings.
 */
public final class CodegenConfig extends Config<CodegenSettings> {

    private final Project project;

    private final SignalConfig commands;
    private final SignalConfig events;
    private final SignalConfig rejections;
    private final EntityConfig entities;
    private final UuidConfig uuids;
    private final ValidationConfig validation;
    private final Set<MessageGroup> messagesConfigs = new HashSet<>();

    @Internal
    public CodegenConfig(Project project) {
        super(project);
        this.project = checkNotNull(project);
        this.commands = new SignalConfig(project);
        this.events = new SignalConfig(project);
        this.rejections = new SignalConfig(project);
        this.entities = new EntityConfig(project);
        this.uuids = new UuidConfig(project);
        this.validation = new ValidationConfig(project);
        applyConvention();
    }

    /**
     * Sets up default values for the options.
     *
     * <p>Default values are based on the Gradle conventions principle.
     * If the user changes the value, the default is completely overridden,
     * even if the used invokes a method which has a name related to appending,
     * e.g. {@code add()}.
     */
    private void applyConvention() {
        commands.convention(COMMANDS, CommandMessage.class);
        events.convention(EVENTS, EventMessage.class, EventMessageField.class);
        rejections.convention(REJECTIONS, RejectionMessage.class, EventMessageField.class);
        entities.convention(OptionsProto.entity, EntityState.class, EntityStateField.class);
        uuids.convention(UuidMethodFactory.class, UuidValue.class);
    }

    /**
     * Configures code generation for command messages.
     */
    public void forCommands(Action<SignalConfig> action) {
        action.execute(commands);
    }

    /**
     * Configures code generation for event messages.
     *
     * <p>Configuration applied to events does not automatically apply to rejections as well.
     */
    public void forEvents(Action<SignalConfig> action) {
        action.execute(events);
    }

    /**
     * Configures code generation for rejection messages.
     *
     * <p>Configuration applied to events does not automatically apply to rejections as well.
     */
    public void forRejections(Action<SignalConfig> action) {
        action.execute(rejections);
    }

    /**
     * Configures code generation for entity state messages.
     */
    public void forEntities(Action<EntityConfig> action) {
        action.execute(entities);
    }

    /**
     * Configures code generation for UUID messages.
     */
    public void forUuids(Action<UuidConfig> action) {
        action.execute(uuids);
    }

    /**
     * Configures code generation for validation messages.
     */
    public void validation(Action<ValidationConfig> action) {
        action.execute(validation);
    }

    /**
     * Configures code generation for a group messages.
     *
     * <p>The group is defined by a file-based selector.
     *
     * @see #by() for creating a file pattern for selecting messages
     */
    public void forMessages(FilePattern filePattern, Action<MessageGroupConfig> action) {
        var pattern = Pattern.newBuilder()
                .setFile(filePattern)
                .build();
        var config = new MessageGroupConfig(project, pattern);
        action.execute(config);
        messagesConfigs.add(config.toProto());
    }

    /**
     * Obtains an instance of {@link FilePatternFactory} which creates file patterns.
     *
     * @see #forMessages
     */
    public FilePatternFactory by() {
        return FilePatternFactory.INSTANCE;
    }

    /**
     * Configures code generation for particular message.
     */
    public void forMessage(String protoTypeName, Action<MessageGroupConfig> action) {
        var name = ProtoTypeName.newBuilder()
                .setValue(protoTypeName)
                .build();
        var pattern = Pattern.newBuilder()
                .setType(TypePattern.newBuilder().setExpectedType(name))
                .build();
        var config = new MessageGroupConfig(project, pattern);
        action.execute(config);
        messagesConfigs.add(config.toProto());
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored") // calling builder
    public CodegenSettings toProto() {
        var signalSettings = SignalSettings.newBuilder()
                .setCommands(commands.toProto())
                .setEvents(events.toProto())
                .setRejections(rejections.toProto())
                .build();

        var groupSettings = GroupSettings.newBuilder();
        messagesConfigs.forEach(groupSettings::addGroup);

        var classpath = buildClasspath();

        var builder = CodegenSettings.newBuilder()
                .setSignalSettings(signalSettings)
                .setGroupSettings(groupSettings)
                .setEntities(entities.toProto())
                .setValidation(validation.toProto())
                .setUuids(uuids.toProto())
                .setClasspath(classpath);
        return builder.build();
    }

    private Classpath buildClasspath() {
        var classpath = Classpath.newBuilder();
        Collection<JavaCompile> javaCompileViews =
                project.getTasks()
                        .withType(JavaCompile.class);
        ImmutableList.copyOf(javaCompileViews)
                .stream()
                .map(JavaCompile::getClasspath)
                .map(FileCollection::getFiles)
                .flatMap(Set::stream)
                .map(File::getAbsolutePath)
                .forEach(classpath::addItem);
        return classpath.build();
    }

    /**
     * Obtains the configuration settings for the generated validation code.
     */
    public ValidationConfig validation() {
        return validation;
    }

    /**
     * Obtains the configuration settings for the generated command code.
     */
    public SignalConfig commands() {
        return commands;
    }

    /**
     * Obtains the configuration settings for the generated event code.
     */
    public SignalConfig events() {
        return events;
    }

    /**
     * Obtains the configuration settings for the generated rejection code.
     */
    public SignalConfig rejections() {
        return rejections;
    }

    /**
     * Obtains the configuration settings for the generated entity code.
     */
    public EntityConfig entities() {
        return entities;
    }

    /**
     * Obtains the configuration settings for the generated identifier types based on UUIDs.
     */
    public UuidConfig uuids() {
        return uuids;
    }
}
