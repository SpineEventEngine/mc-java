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

package io.spine.tools.mc.java.annotation.check;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jboss.forge.roaster.model.impl.AbstractJavaSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.util.Optional;

import static io.spine.tools.mc.java.annotation.check.Annotations.findInternalAnnotation;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a nested type is annotated.
 */
public final class NestedTypesAnnotationCheck extends SourceCheck {

    public NestedTypesAnnotationCheck(boolean shouldBeAnnotated) {
        super(shouldBeAnnotated);
    }

    @Override
    public void accept(@Nullable AbstractJavaSource<JavaClassSource> outerClass) {
        requireNonNull(outerClass);
        for (var nestedType : outerClass.getNestedTypes()) {
            Optional<?> annotation = findInternalAnnotation(nestedType);
            if (shouldBeAnnotated()) {
                assertTrue(annotation.isPresent(),
                           format("`%s` is not annotated.", nestedType.getQualifiedName()));
            } else {
                assertFalse(annotation.isPresent(),
                            format("`%s` is annotated.", nestedType.getQualifiedName()));
            }
        }
    }
}
