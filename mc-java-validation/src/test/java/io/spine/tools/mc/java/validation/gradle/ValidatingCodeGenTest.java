/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.validation.gradle;

import io.spine.tools.gradle.testing.GradleProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static io.spine.tools.gradle.task.JavaTaskName.compileJava;

@DisplayName("Validation code generation should")
@Disabled("These tests should belong to the Validation library and simply be moved there." +
        " McJava should have a smoke test on Validation library, but not these tests" +
        " that consume much of build time.") // https://github.com/SpineEventEngine/mc-java/issues/119
class ValidatingCodeGenTest {

    private static GradleProject project = null;

    @BeforeAll
    static void createProject(@TempDir Path tempDir) {
        var projectDir = tempDir.toFile();
        project = GradleProject
                .setupAt(projectDir)
                .fromResources("validation-gen-plugin-test")
                .copyBuildSrc()
                .create();
    }

    @Test
    @DisplayName("generate valid Java code")
    void generatingJavaCode() {
        project.executeTask(compileJava);
    }
}
