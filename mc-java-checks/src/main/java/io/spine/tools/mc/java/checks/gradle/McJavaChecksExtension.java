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

package io.spine.tools.mc.java.checks.gradle;

import io.spine.tools.mc.checks.Severity;
import org.gradle.api.Project;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Allows configuring severity for all the Spine Java Checks applied to the project.
 *
 * @see Severity
 */
@SuppressWarnings("PublicField" /* required for exposing the property in Gradle. */)
public class McJavaChecksExtension {

    private static final String EXTENSION_NAME = "modelChecks";

    public Severity useValidatingBuilderSeverity;

    /**
     * Creates an instance of the extension in the given project.
     */
    static void createIn(Project project) {
        checkNotNull(project);
        project.getExtensions()
               .create(name(), McJavaChecksExtension.class);
    }

    public static Severity getUseValidatingBuilderSeverity(Project project) {
        var extension = (McJavaChecksExtension) project.getExtensions().getByName(name());
        return extension.useValidatingBuilderSeverity;
    }

    public static String name() {
        return EXTENSION_NAME;
    }
}
