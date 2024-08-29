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
import io.spine.protodata.FilePattern;
import io.spine.tools.mc.java.settings.Signals;
import org.checkerframework.checker.signature.qual.FqBinaryName;
import org.gradle.api.Project;

/**
 * Code generation config for a type of signal messages.
 *
 * <p>May configure all the events, all the rejections, or all the commands.
 *
 * <p>Settings applied to events do not automatically apply to rejections.
 */
public final class SignalSettings extends GroupedByFilePatterns<Signals> {

    private static final String FIELD_ACTION =
            "io.spine.tools.mc.java.signal.AddEventMessageField";

    /**
     * Default codegen action for command messages.
     */
    @VisibleForTesting
    public static final ImmutableList<@FqBinaryName String> DEFAULT_COMMAND_ACTIONS =
            ImmutableList.of(
                    "io.spine.tools.mc.java.signal.ImplementCommandMessage"
            );

    /**
     * Default codegen action for event messages.
     */
    @VisibleForTesting
    public static final ImmutableList<@FqBinaryName String> DEFAULT_EVENT_ACTIONS =
            ImmutableList.of(
                    "io.spine.tools.mc.java.signal.ImplementEventMessage",
                    FIELD_ACTION
            );

    /**
     * Default codegen action for rejection messages.
     */
    @VisibleForTesting
    public static final ImmutableList<@FqBinaryName String> DEFAULT_REJECTION_ACTIONS =
            ImmutableList.of(
                    "io.spine.tools.mc.java.signal.ImplementRejectionMessage",
                    FIELD_ACTION
            );

    /**
     * Creates a new instance under the given project.
     *
     * @param project
     *         the project under which settings are created
     * @param suffix
     *         the default file suffix to initialize the file filtering pattern in conventions
     * @param defaultActions
     *         code generation actions to be executed for this kind of signals
     */
    SignalSettings(Project project,
                   String suffix,
                   Iterable<@FqBinaryName String> defaultActions) {
        super(project, defaultActions);
        var pattern = FilePattern.newBuilder()
                .setSuffix(suffix)
                .build();
        convention(pattern);
    }

    @Override
    public Signals toProto() {
        return Signals.newBuilder()
                .addAllPattern(patterns())
                .setActions(actions())
                .build();
    }
}
