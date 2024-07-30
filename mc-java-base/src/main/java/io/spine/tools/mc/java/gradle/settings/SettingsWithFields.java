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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.tools.mc.java.settings.GenerateFields;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FqBinaryName;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

import static io.spine.tools.java.code.Names.className;

/**
 * Code generation settings that include generation of
 * {@linkplain io.spine.base.SubscribableField field classes}.
 *
 * <p>Model Compiler generates type-safe API for filtering messages by fields in queries
 * and subscriptions.
 *
 * @param <P>
 *         Protobuf type reflecting a snapshot of these settings
 */
public abstract class SettingsWithFields<P extends Message> extends SettingsWithActions<P> {

    private final Property<String> markFieldsAs;

    SettingsWithFields(Project p, Iterable<@FqBinaryName String> defaultActions) {
        super(p, defaultActions);
        markFieldsAs = p.getObjects().property(String.class);
    }

    /**
     * Creates an instance of settings for the given project.
     */
    SettingsWithFields(Project p) {
        this(p, ImmutableList.of());
    }

    /**
     * Sets up the default state for the {@code Field} class generation config.
     *
     * <p>If a class is provided, the {@code Field} class will be generated with the given class
     * as a supertype.
     *
     * <p>If the {@code fieldSuperclass} is {@code null}, the {@code Field} class will not be
     * generated.
     */
    final void convention(@Nullable Class<?> fieldSuperclass) {
        if (fieldSuperclass != null) {
            markFieldsAs.convention(fieldSuperclass.getCanonicalName());
        }
    }

    /**
     * Equips the field type with a superclass.
     *
     * @param className
     *         the canonical class name of an existing Java class
     */
    public final void markFieldsAs(String className) {
        markFieldsAs.set(className);
    }

    /**
     * Obtains the {@link GenerateFields} instance containing specified names of
     * field superclasses.
     */
    final GenerateFields generateFields() {
        GenerateFields generateFields;
        var superclassName = markFieldsAs.getOrElse("");
        if (superclassName.isEmpty()) {
            generateFields = GenerateFields.getDefaultInstance();
        } else {
            generateFields = GenerateFields.newBuilder()
                    .setSuperclass(className(superclassName))
                    .build();
        }
        return generateFields;
    }
}
