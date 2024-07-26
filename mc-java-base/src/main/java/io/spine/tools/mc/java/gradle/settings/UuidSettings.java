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
import io.spine.tools.mc.java.settings.Uuids;
import org.gradle.api.Project;

/**
 * Settings for code generation for messages that qualify as {@link io.spine.base.UuidValue}.
 */
public final class UuidSettings extends SettingsWithInterfaces<Uuids> {

    /**
     * The name of the default codegen action applied to {@link io.spine.base.UuidValue}s.
     */
    @VisibleForTesting
    public static final ImmutableList<String> DEFAULT_ACTIONS = ImmutableList.of(
            "io.spine.tools.mc.java.uuid.ImplementUuidValue",
            "io.spine.tools.mc.java.uuid.AddFactoryMethods"
    );

    UuidSettings(Project p) {
        super(p, DEFAULT_ACTIONS);
    }

    @Override
    public Uuids toProto() {
        return Uuids.newBuilder()
                .addAllAction(actions())
                .build();
    }

    /**
     * Does nothing.
     *
     * @deprecated Please use {@link SettingsWithInterfaces#useAction(String)} instead.
     */
    @Deprecated
    public void generateMethodsWith(@SuppressWarnings("unused") String factoryClassName) {
        // Does nothing.
    }
}
