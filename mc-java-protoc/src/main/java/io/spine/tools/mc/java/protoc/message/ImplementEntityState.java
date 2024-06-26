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

package io.spine.tools.mc.java.protoc.message;

import com.google.common.collect.ImmutableList;
import io.spine.tools.java.code.JavaClassName;
import io.spine.tools.mc.java.settings.Entities;
import io.spine.tools.mc.java.protoc.CompilerOutput;
import io.spine.tools.mc.java.protoc.EntityMatcher;
import io.spine.type.MessageType;

import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Marks the provided message type with the {@link io.spine.base.EntityState EntityState} interface
 * if the type is recognized as entity state.
 */
final class ImplementEntityState extends ImplementInterface {

    private final Predicate<MessageType> matcher;

    ImplementEntityState(JavaClassName interfaceName, Entities config) {
        super(interfaceName);
        this.matcher = new EntityMatcher(config);
    }

    @Override
    public ImmutableList<CompilerOutput> generateFor(MessageType type) {
        checkNotNull(type);
        if (matcher.test(type)) {
            return super.generateFor(type);
        }
        return ImmutableList.of();
    }

    @Override
    public InterfaceParameters interfaceParameters(MessageType type) {
        var idType = firstFieldOf(type);
        return InterfaceParameters.of(idType);
    }

    private static InterfaceParameter firstFieldOf(MessageType type) {
        var fields = type.fields();
        checkState(fields.size() > 0,
                   "At least one field is required in an `EntityState` message type.");
        var declaration = fields.get(0);
        var value = declaration.className();
        return new ExistingClass(value);
    }
}
