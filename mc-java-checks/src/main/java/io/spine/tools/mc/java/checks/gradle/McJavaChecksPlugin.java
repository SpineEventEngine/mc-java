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
package io.spine.tools.mc.java.checks.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * A Gradle plugin which configures the project to run Model Compiler Checks for Java during the
 * compilation stage.
 *
 * <p>To work, this plugin requires <a href="https://github.com/tbroyer/gradle-errorprone-plugin">
 * the Error Prone plugin</a> to be applied to the project.
 *
 * <p>The plugin adds
 * a {@link io.spine.tools.mc.java.checks.Artifacts#MC_JAVA_CHECKS_ARTIFACT mc-java-checks}
 * dependency to the {@code annotationProcessor} configuration of a Gradle project.
 * For the older Gradle versions (pre {@code 4.6}), where there is no such configuration,
 * the plugin creates it.
 */
public final class McJavaChecksPlugin implements Plugin<Project> {

    /**
     * Applies the plugin to the given {@code Project}.
     *
     * @param project
     *         the project to apply the plugin to
     */
    @Override
    public void apply(Project project) {
        McJavaChecksExtension.createIn(project);
        var dependencyResolved = McJavaChecksDependency.addTo(project);
        if (!dependencyResolved) {
            return;
        }

        var severity = McJavaChecksSeverity.initFor(project);
        severity.addConfigureSeverityAction();
    }
}
