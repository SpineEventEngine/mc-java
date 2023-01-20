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

package io.spine.tools.mc.java.rejection.gradle;

import com.google.common.collect.ImmutableList;
import io.spine.tools.code.SourceSetName;
import io.spine.tools.gradle.task.GradleTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.tools.gradle.project.Projects.getSourceSetNames;
import static io.spine.tools.gradle.task.JavaTaskName.compileJava;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.generateRejections;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.mergeDescriptorSet;

/**
 * Plugin which generates Rejections declared in {@code rejections.proto} files.
 *
 * <p>Uses generated proto descriptors.
 *
 * <p>Logs a warning if there are no protobuf descriptors generated.
 */
public final class RejectionGenPlugin implements Plugin<Project> {

    /**
     * Applies the plug-in to a project.
     *
     * <p>Adds {@code :generateRejections} tasks for all source sets of the project
     * after the project is evaluated.
     *
     * <p>Tasks depend on corresponding {@code :generateProto} tasks and are executed
     * before corresponding {@code :compileJava} tasks.
     */
    @Override
    public void apply(Project project) {
        project.afterEvaluate(p -> {
            var tasks = createTasks(p);
            p.getLogger()
             .debug("Rejection generation plugin initialized with tasks: `{}`.", tasks);
        });
    }

    private static ImmutableList<GradleTask> createTasks(Project project) {
        var sourceSetNames = getSourceSetNames(project);
        var gradleTasks = sourceSetNames
                .stream()
                .map(ssn -> createTask(ssn, project))
                .collect(toImmutableList());
        return gradleTasks;
    }

    private static GradleTask createTask(SourceSetName ssn, Project project) {
        var action = RejectionGenAction.create(project, ssn);
        var rejections = generateRejections(ssn);
        var mergeTask = mergeDescriptorSet(ssn);
        var compileTask = compileJava(ssn);
        var task = GradleTask.newBuilder(rejections, action)
                .insertBeforeTask(compileTask)
                .insertAfterTask(mergeTask)
                .applyNowTo(project);
        return task;
    }
}
