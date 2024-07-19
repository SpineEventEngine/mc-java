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
import io.spine.base.UuidValue;
import io.spine.tools.gradle.Multiple;
import io.spine.tools.java.code.UuidMethodFactory;
import io.spine.tools.mc.java.settings.MethodFactoryName;
import io.spine.tools.mc.java.settings.Uuids;
import org.gradle.api.Project;

import java.util.Set;

import static io.spine.tools.java.code.Names.className;

/**
 * Settings for code generation for messages that qualify as {@link io.spine.base.UuidValue}.
 */
public final class UuidSettings extends SettingsWithInterfaces<Uuids> {

    private final Multiple<String> methodFactories;

    UuidSettings(Project p) {
        super(p);
        methodFactories = new Multiple<>(p, String.class);
        methodFactories.convention(ImmutableSet.of(UuidMethodFactory.class.getCanonicalName()));
        interfaceNames().convention(ImmutableSet.of(UuidValue.class.getCanonicalName()));
    }

    @Override
    public Uuids toProto() {
        return Uuids.newBuilder()
                .addAllMethodFactory(factories())
                .addAllAddInterface(interfaces())
                .addAllAction(actions())
                .build();
    }

    /**
     * Specifies a {@link io.spine.tools.java.code.MethodFactory} to generate methods for
     * the UUID message classes.
     *
     * <p>Calling this method multiple times will add provide factories for code generation.
     *
     * @param factoryClassName
     *         the canonical class name of the method factory
     */
    public void generateMethodsWith(String factoryClassName) {
        methodFactories.add(factoryClassName);
    }

    private Set<MethodFactoryName> factories() {
        return methodFactories.transform(name -> MethodFactoryName.newBuilder()
                .setClassName(className(name))
                .build());
    }
}
