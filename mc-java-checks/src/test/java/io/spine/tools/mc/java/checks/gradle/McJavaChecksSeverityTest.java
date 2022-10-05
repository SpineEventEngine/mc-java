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

import io.spine.testing.logging.mute.MuteLogging;
import io.spine.tools.mc.gradle.ModelCompilerOptions;
import io.spine.tools.mc.java.checks.gradle.given.ProjectConfigurations;
import io.spine.tools.mc.java.checks.gradle.given.StubProject;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.tools.mc.checks.Severity.ERROR;
import static io.spine.tools.mc.java.checks.gradle.McJavaChecksSeverity.EQUALITY_ERROR;
import static io.spine.tools.mc.java.checks.gradle.McJavaChecksSeverity.ERROR_PRONE_PLUGIN_ID;

/**
 * Tests {@link io.spine.tools.gradle.compiler.Severity}.
 */
@DisplayName("`McJavaChecksSeverity` should")
class McJavaChecksSeverityTest {

    private Project project;
    private McJavaChecksSeverity configurer;

    @BeforeEach
    void createProject() {
        project = StubProject.createFor(getClass()).get();
        configurer = McJavaChecksSeverity.initFor(project);
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // We use one extension and just create the other one.
    @Test
    @DisplayName("configure check severity")
    @Disabled("Needs to be re-implemented when real checks are added")
    void configureCheckSeverity() {
        project.getPlugins()
               .apply(ERROR_PRONE_PLUGIN_ID);
        configureModelCompilerExtension();
        var extension = configureSpineCheckExtension();
        extension.useValidatingBuilderSeverity = ERROR;
        configurer.setHasErrorPronePlugin(true);
        configurer.addConfigureSeverityAction();
        checkSeverityConfiguredToError();
    }

    @Test
    @DisplayName("not add severity args if Error Prone plugin not applied")
    @MuteLogging
    void detectErrorProne() {
        configurer.setHasErrorPronePlugin(false);
        configurer.addConfigureSeverityAction();
        checkSeverityNotConfigured();
    }

    private McJavaChecksExtension configureSpineCheckExtension() {
        var extensions = project.getExtensions();
        var extension = extensions.create(McJavaChecksExtension.name(),
                                          McJavaChecksExtension.class);
        return extension;
    }

    private ModelCompilerOptions configureModelCompilerExtension() {
        var extensions = project.getExtensions();
        var extension = extensions.create(ModelCompilerOptions.name, ModelCompilerOptions.class);
        return extension;
    }

    private void checkSeverityConfiguredToError() {
        ProjectConfigurations.assertCompileTasksContain(project, EQUALITY_ERROR);
    }

    private void checkSeverityNotConfigured() {
        ProjectConfigurations.assertCompileTasksEmpty(project);
    }
}
