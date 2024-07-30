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

import com.google.common.collect.ImmutableSet;
import io.spine.base.MessageFile;
import io.spine.base.SignalMessage;
import io.spine.protodata.FilePattern;
import io.spine.tools.mc.java.settings.Signals;
import org.checkerframework.checker.nullness.qual.Nullable;
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

    SignalSettings(Project p,
                   MessageFile pattern,
                   Class<? extends SignalMessage> interfaceClass,
                   @Nullable Class<?> fieldSuperclass,
                   Iterable<@FqBinaryName String> defaultActions) {
        super(p, defaultActions);
        convention(pattern, interfaceClass, fieldSuperclass);
    }

    /**
     * Sets up default values for the properties of this config.
     *
     * @param file
     *         the type of files associated with this config; used to derive the default
     *         file pattern
     * @param interfaceClass
     *         the default marker interface
     * @param fieldSuperclass
     *         the default superclass for the nested {@code Field} class; {@code null} denotes
     *         not generating a {@code Field} class at all
     */
    private void convention(MessageFile file,
                            Class<? extends SignalMessage> interfaceClass,
                            @Nullable Class<?> fieldSuperclass) {
        var pattern = FilePattern.newBuilder()
                .setSuffix(file.suffix())
                .build();
        convention(pattern);
        this.interfaceNames()
            .convention(ImmutableSet.of(interfaceClass.getCanonicalName()));
        if (fieldSuperclass != null) {
            convention(fieldSuperclass);
        }
    }

    @Override
    public Signals toProto() {
        return Signals.newBuilder()
                .addAllAddInterface(interfaces())
                .setGenerateFields(generateFields())
                .addAllPattern(patterns())
                .build();
    }
}
