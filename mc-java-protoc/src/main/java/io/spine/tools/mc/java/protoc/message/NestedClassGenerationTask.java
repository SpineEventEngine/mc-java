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
import io.spine.tools.java.code.NestedClassFactory;
import io.spine.tools.mc.java.settings.NestedClassFactoryName;
import io.spine.tools.mc.java.protoc.ClassMember;
import io.spine.tools.mc.java.protoc.CodeGenerationTask;
import io.spine.tools.mc.java.protoc.CompilerOutput;
import io.spine.tools.mc.java.protoc.ExternalClassLoader;
import io.spine.type.MessageType;
import org.checkerframework.checker.nullness.qual.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.util.Preconditions2.checkNotDefaultArg;

/**
 * An abstract base for the nested classes generation tasks.
 */
abstract class NestedClassGenerationTask implements CodeGenerationTask {

    private final ExternalClassLoader<NestedClassFactory> classLoader;
    private final NestedClassFactoryName factoryName;

    NestedClassGenerationTask(ExternalClassLoader<NestedClassFactory> classLoader,
                              NestedClassFactoryName factoryName) {
        this.classLoader = checkNotNull(classLoader);
        this.factoryName = checkNotDefaultArg(factoryName);
    }

    /**
     * Performs the actual code generation using the supplied {@linkplain #factoryName factory}.
     */
    ImmutableList<CompilerOutput> generateNestedClassesFor(@NonNull MessageType type) {
        var className = factoryName.getClassName().getCanonical();
        var factory = classLoader.newInstance(className);
        return factory
                .generateClassesFor(type)
                .stream()
                .map(classBody -> ClassMember.nestedClass(classBody, type))
                .collect(toImmutableList());
    }
}
