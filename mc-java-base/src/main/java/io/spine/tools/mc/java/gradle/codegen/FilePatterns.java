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

package io.spine.tools.mc.java.gradle.codegen;

import io.spine.validation.FilePattern;
import org.checkerframework.checker.regex.qual.Regex;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An utility for working with {@link FilePattern}.
 */
public final class FilePatterns {

    /** Prevents instantiation of this utility class. */
    private FilePatterns() {
    }

    /**
     * Creates a new {@link FilePattern} with a {@code suffix} field filled.
     */
    public static FilePattern fileSuffix(@Regex String suffix) {
        checkNotNull(suffix);
        return FilePattern.newBuilder()
                          .setSuffix(suffix)
                          .build();
    }

    /**
     * Creates a new {@link FilePattern} with a {@code prefix} field filled.
     */
    public static FilePattern filePrefix(@Regex String prefix) {
        checkNotNull(prefix);
        return FilePattern.newBuilder()
                          .setPrefix(prefix)
                          .build();
    }

    /**
     * Creates a new {@link FilePattern} with a {@code regex} field filled.
     */
    public static FilePattern fileRegex(@Regex String regex) {
        checkNotNull(regex);
        return FilePattern.newBuilder()
                          .setRegex(regex)
                          .build();
    }
}
