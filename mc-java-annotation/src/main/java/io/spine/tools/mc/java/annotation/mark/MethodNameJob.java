/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.annotation.mark;

import com.google.common.collect.ImmutableSet;
import io.spine.code.java.ClassName;
import io.spine.logging.WithLogging;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * An annotation {@link Job} which annotates methods matching certain naming patterns.
 */
final class MethodNameJob extends AnnotationJob implements WithLogging {

    private final ImmutableSet<MethodPattern> patterns;

    MethodNameJob(ImmutableSet<MethodPattern> patterns, ClassName annotation) {
        super(annotation);
        this.patterns = checkNotNull(patterns);
    }

    @Override
    public void execute(AnnotatorFactory factory) {
        var annotation = annotation();
        logger().atDebug().log(() -> format(
                "Annotating methods matching patterns `%s` with `%s`.", patterns, annotation));
        factory.createMethodAnnotator(annotation, patterns)
               .annotate();
    }
}
