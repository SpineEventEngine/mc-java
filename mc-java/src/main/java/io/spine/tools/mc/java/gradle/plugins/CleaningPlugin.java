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
package io.spine.tools.mc.java.gradle.plugins;

import com.google.common.base.Joiner;
import io.spine.tools.gradle.task.GradleTask;
import io.spine.tools.mc.java.gradle.TempArtifactDirs;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;

import static io.spine.io.Delete.deleteRecursively;
import static io.spine.tools.gradle.task.BaseTaskName.clean;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.preClean;

/**
 * Plugin which performs additional cleanup of the Spine-generated folders.
 *
 * <p>Adds a custom `:preClean` task, which is executed before the `:clean` task.
 */
public final class CleaningPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        var preCleanAction = new PreCleanAction(project);
        var preCleanTask = GradleTask.newBuilder(preClean, preCleanAction)
                .insertBeforeTask(clean)
                .applyNowTo(project);
        project.getLogger().debug("Pre-clean phase initialized: `{}`.", preCleanTask);
    }

    /**
     * Recursively deletes {@linkplain TempArtifactDirs temp. artifact directories} in
     * the given project.
     */
    private static class PreCleanAction implements Action<Task> {

        private final Project project;

        private PreCleanAction(Project project) {
            this.project = project;
        }

        @Override
        public void execute(Task task) {
            var logger = project.getLogger();
            var dirsToClean = TempArtifactDirs.getFor(project);
            if (logger.isDebugEnabled()) {
                var dirs = Joiner.on(", ").join(dirsToClean);
                logger.debug("Pre-clean: deleting the directories (`{}`).", dirs);
            }
            dirsToClean.stream()
                    .map(File::toPath)
                    .forEach(dir -> {
                        logger.debug("Deleting directory `{}`...", dir);
                        deleteRecursively(dir);
                    });
        }
    }
}

