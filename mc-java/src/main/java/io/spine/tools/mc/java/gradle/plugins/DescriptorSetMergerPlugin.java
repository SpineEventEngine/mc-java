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

import io.spine.tools.code.SourceSetName;
import io.spine.tools.gradle.task.GradleTask;
import io.spine.tools.type.FileDescriptorSuperset;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import static io.spine.tools.gradle.JavaConfigurationName.runtimeClasspath;
import static io.spine.tools.gradle.project.Projects.configuration;
import static io.spine.tools.gradle.project.Projects.getSourceSetNames;
import static io.spine.tools.gradle.protobuf.Projects.descriptorSetFile;
import static io.spine.tools.gradle.task.JavaTaskName.processResources;
import static io.spine.tools.gradle.task.ProtobufTaskName.generateProto;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.mergeDescriptorSet;

/**
 * A Gradle plugin which merges the descriptor file with all the descriptor files from
 * the project runtime classpath.
 *
 * <p>The merge result is used to {@linkplain
 * io.spine.tools.type.MoreKnownTypes#extendWith(java.io.File) extend the known type registry}.
 */
final class DescriptorSetMergerPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        getSourceSetNames(project).forEach(ssn -> createTask(project, ssn));
    }

    private static void createTask(Project project, SourceSetName ssn) {
        var configuration = configuration(project, runtimeClasspath(ssn));
        var dependencies = configuration.getAllDependencies();
        var action = createMergingAction(ssn);
        var task = GradleTask.newBuilder(mergeDescriptorSet(ssn), action)
                .insertAfterTask(generateProto(ssn))
                .insertBeforeTask(processResources(ssn))
                .applyNowTo(project);
        task.getTask().dependsOn(dependencies);
    }

    private static Action<Task> createMergingAction(SourceSetName ssn) {
        return task -> {
            var superset = new FileDescriptorSuperset();
            var project = task.getProject();
            var configuration = configuration(project, runtimeClasspath(ssn));
            configuration.forEach(superset::addFromDependency);
            var descriptorSet = descriptorSetFile(project, ssn);
            if (descriptorSet.exists()) {
                superset.addFromDependency(descriptorSet);
            }
            superset.merge()
                    .loadIntoKnownTypes();
        };
    }
}
