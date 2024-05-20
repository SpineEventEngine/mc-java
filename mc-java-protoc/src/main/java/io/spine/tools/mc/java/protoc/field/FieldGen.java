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

package io.spine.tools.mc.java.protoc.field;

import com.google.common.collect.ImmutableList;
import io.spine.tools.java.code.field.FieldFactory;
import io.spine.tools.mc.java.settings.CodegenOptions;
import io.spine.tools.mc.java.settings.MessageGroup;
import io.spine.tools.mc.java.settings.SignalSettings;
import io.spine.tools.mc.java.settings.Signals;
import io.spine.tools.mc.java.protoc.CodeGenerationTask;
import io.spine.tools.mc.java.protoc.CodeGenerationTasks;
import io.spine.tools.mc.java.protoc.CodeGenerator;
import io.spine.tools.mc.java.protoc.CompilerOutput;
import io.spine.tools.mc.java.protoc.InsertionPoint;
import io.spine.type.MessageType;
import io.spine.type.Type;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * A code generator which adds the strongly-typed fields to a message type.
 *
 * <p>The generator produces {@link CompilerOutput compiler output} that fits into the message's
 * {@link InsertionPoint#class_scope class_scope} insertion point.
 */
public final class FieldGen extends CodeGenerator {

    /**
     * The factory used for code generation.
     */
    private static final FieldFactory factory = new FieldFactory();

    private final CodeGenerationTasks codeGenerationTasks;

    private FieldGen(Builder builder) {
        super();
        this.codeGenerationTasks = new CodeGenerationTasks(builder.tasks());
    }

    /**
     * Creates a new instance based on the passed Protoc config.
     */
    public static FieldGen instance(CodegenOptions config) {
        checkNotNull(config);
        var builder = new Builder(config);
        builder.addFromAll();
        return builder.build();
    }

    @Override
    protected Collection<CompilerOutput> generate(Type<?, ?> type) {
        if (!(type instanceof MessageType)) {
            return ImmutableList.of();
        }
        var messageType = (MessageType) type;
        var result = codeGenerationTasks.generateFor(messageType);
        return result;
    }

    /**
     * A builder for the {@code FieldGen} instances.
     */
    private static final class Builder {

        private final CodegenOptions config;
        private final SignalSettings signalSettings;
        private final ImmutableList.Builder<CodeGenerationTask> tasks = ImmutableList.builder();

        /**
         * Prevents direct instantiation.
         */
        private Builder(CodegenOptions config) {
            this.config = config;
            this.signalSettings = config.getSignalSettings();
        }

        private ImmutableList<CodeGenerationTask> tasks() {
            return tasks.build();
        }

        /**
         * Creates a new instance of {@code FieldGen}.
         *
         * @return new instance of {@code FieldGen}
         */
        private FieldGen build() {
            return new FieldGen(this);
        }

        private void addFromAll() {
            addFromCommands();
            addFromEvents();
            addFromRejections();
            addFromEntities();
            addFromMessages();
        }

        private void addFromMessages() {
            for (var group : config.getGroupSettings().getGroupList()) {
                taskFor(group).ifPresent(tasks::add);
            }
        }

        private void addFromRejections() {
            if (signalSettings.hasRejections()) {
                var signals = signalSettings.getRejections();
                tasks.addAll(tasksFor(signals));
            }
        }

        private void addFromEvents() {
            if (signalSettings.hasEvents()) {
                var signals = signalSettings.getEvents();
                tasks.addAll(tasksFor(signals));
            }
        }

        private void addFromCommands() {
            if (signalSettings.hasCommands()) {
                var signals = signalSettings.getCommands();
                tasks.addAll(tasksFor(signals));
            }
        }

        private void addFromEntities() {
            if (config.hasEntities()) {
                var entities = config.getEntities();
                var fields = entities.getGenerateFields();
                if (fields.hasSuperclass()) {
                    tasks.add(new GenerateEntityStateFields(entities, factory));
                }
            }
        }

        private static ImmutableList<GenerateFieldsByPattern> tasksFor(Signals signals) {
            var generateFields = signals.getGenerateFields();
            if (!generateFields.hasSuperclass()) {
                return ImmutableList.of();
            }
            return signals.getPatternList()
                          .stream()
                          .map(filePattern -> new GenerateFieldsByPattern(
                                  generateFields, filePattern, factory
                          )).collect(toImmutableList());
        }

        private static Optional<GenerateFieldsByPattern> taskFor(MessageGroup messages) {
            var generateFields = messages.getGenerateFields();
            if (!generateFields.hasSuperclass()) {
                return Optional.empty();
            }
            var pattern = messages.getPattern();
            var task = new GenerateFieldsByPattern(generateFields, pattern, factory);
            return Optional.of(task);
        }
    }
}
