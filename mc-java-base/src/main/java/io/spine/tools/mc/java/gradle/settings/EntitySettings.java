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

package io.spine.tools.mc.java.gradle.settings;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.base.EntityState;
import io.spine.option.OptionsProto;
import io.spine.query.EntityStateField;
import io.spine.tools.mc.java.settings.Entities;
import io.spine.tools.proto.code.ProtoOption;
import org.checkerframework.checker.signature.qual.FqBinaryName;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Configuration for entity state types' code generation.
 *
 * @see CodegenSettings#forEntities(Action)
 */
public final class EntitySettings extends SettingsWithFields<Entities> {

    /**
     * Names of render action classes applied by default to entity states.
     */
    @VisibleForTesting
    public static final ImmutableList<@FqBinaryName String> DEFAULT_ACTIONS = ImmutableList.of(
        "io.spine.tools.mc.java.entity.column.AddColumnClass",
        "io.spine.tools.mc.java.entity.field.AddFieldClass",
        "io.spine.tools.mc.java.entity.query.AddQuerySupport",
        "io.spine.tools.mc.java.entity.ImplementEntityState"
    );

    private final SetProperty<String> options;
    private final Property<Boolean> generateQueries;

    @VisibleForTesting
    public EntitySettings(Project p) {
        super(p, DEFAULT_ACTIONS);
        convention(EntityStateField.class);
        interfaceNames().convention(ImmutableSet.of(
                EntityState.class.getCanonicalName()
        ));
        options = p.getObjects().setProperty(String.class);
        options.convention(ImmutableSet.of(
                OptionsProto.entity.getDescriptor().getName()
        ));
        generateQueries = p.getObjects().property(Boolean.class);
        generateQueries.convention(true);
    }

    /**
     * The Protobuf options which mark entity states.
     *
     * <p>By default, the {@code (entity)} option is used.
     *
     * <p>Note. This is a part of the advanced level API.
     * See the <a href="#disclaimer">disclaimer</a> above.
     */
    @Internal
    public SetProperty<String> getOptions() {
        return options;
    }

    /**
     * Enables type-safe query API generation for entity states.
     */
    public void generateQueries() {
        generateQueries.set(true);
    }

    /**
     * Disables type-safe query API generation for entity states.
     */
    public void skipQueries() {
        generateQueries.set(false);
    }

    @Override
    public Entities toProto() {
        return Entities.newBuilder()
                .addAllOption(options())
                .setGenerateQueries(generateQueries.get())
                .setActions(actions())
                .build();
    }

    private List<ProtoOption> options() {
        return options.get()
                      .stream()
                      .map(name -> ProtoOption.newBuilder()
                              .setName(name)
                              .build())
                      .collect(toList());
    }
}
