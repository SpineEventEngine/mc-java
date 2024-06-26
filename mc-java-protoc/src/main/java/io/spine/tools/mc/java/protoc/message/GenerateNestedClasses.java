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
import io.spine.tools.mc.java.settings.Pattern;
import io.spine.tools.mc.java.protoc.CompilerOutput;
import io.spine.tools.mc.java.protoc.ExternalClassLoader;
import io.spine.tools.mc.java.protoc.PatternMatcher;
import io.spine.type.MessageType;
import io.spine.validation.FilePattern;

import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Preconditions2.checkNotDefaultArg;

/**
 * Generates nested classes for the supplied type based on
 * a {@link FilePattern pattern} configuration.
 */
public final class GenerateNestedClasses extends NestedClassGenerationTask {

    private final Predicate<MessageType> patternMatcher;

    public GenerateNestedClasses(ExternalClassLoader<NestedClassFactory> classLoader,
                                 NestedClassFactoryName factoryName,
                                 Pattern pattern) {
        super(classLoader, factoryName);
        checkNotDefaultArg(pattern);
        this.patternMatcher = new PatternMatcher(pattern);
    }

    /**
     * Generates nested classes for the given type.
     *
     * <p>No code is generated if the type file name does not match the supplied
     * {@link io.spine.validation.FilePattern pattern}.
     */
    @Override
    public ImmutableList<CompilerOutput> generateFor(MessageType type) {
        checkNotNull(type);
        if (!patternMatcher.test(type)) {
            return ImmutableList.of();
        }
        return generateNestedClassesFor(type);
    }
}
