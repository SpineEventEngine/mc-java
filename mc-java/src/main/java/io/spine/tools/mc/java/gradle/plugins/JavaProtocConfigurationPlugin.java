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

package io.spine.tools.mc.java.gradle.plugins;

import com.google.protobuf.gradle.ExecutableLocator;
import com.google.protobuf.gradle.GenerateProtoTask;
import io.spine.code.proto.DescriptorReference;
import io.spine.tools.code.SourceSetName;
import io.spine.tools.gradle.ProtocConfigurationPlugin;
import io.spine.tools.gradle.task.GradleTask;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;

import static io.spine.tools.gradle.ProtocPluginName.grpc;
import static io.spine.tools.gradle.task.JavaTaskName.processResources;
import static io.spine.tools.gradle.task.Tasks.getSourceSetName;
import static io.spine.tools.mc.java.gradle.Artifacts.gRpcProtocPlugin;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.writeDescriptorReference;

/**
 * A Gradle plugin that performs additional {@code protoc} configurations relevant
 * for Java projects.
 */
public final class JavaProtocConfigurationPlugin extends ProtocConfigurationPlugin {

    @Override
    protected void
    configureProtocPlugins(NamedDomainObjectContainer<ExecutableLocator> plugins, Project project) {
        plugins.create(grpc.name(),
                       locator -> locator.setArtifact(gRpcProtocPlugin().notation())
        );
    }

    @Override
    protected void customizeTask(GenerateProtoTask protocTask) {
        var helper = new Helper(protocTask);
        helper.configure();
    }

    /**
     * A method object configuring an instance of {@code GenerateProtoTask}.
     *
     * @see #customizeTask(GenerateProtoTask)
     */
    private static class Helper {

        private final Project project;
        private final GenerateProtoTask protocTask;
        private final SourceSetName sourceSetName;
        private final File descriptorFile;

        private Helper(GenerateProtoTask task) {
            this.project = task.getProject();
            this.protocTask = task;
            this.sourceSetName = getSourceSetName(protocTask);
            this.descriptorFile = new File(protocTask.getDescriptorPath());
        }

        private void configure() {
            customizeDescriptorSetGeneration();
            addPlugins();
        }

        private void customizeDescriptorSetGeneration() {
            setResourceDirectory();
            var taskName = writeDescriptorReference(sourceSetName);
            var writeRef = GradleTask.newBuilder(taskName, writeRefFile())
                    .insertBeforeTask(processResources(sourceSetName))
                    .applyNowTo(project);

            var log = project.getLogger();
            log.debug("McJava: The task `{}` has been created.", writeRef.getName());

            protocTask.finalizedBy(writeRef.getTask());

            log.debug("McJava: The task `{}` is configured to be finalized by `{}`.",
                       protocTask.getName(), writeRef.getName());
        }

        private void setResourceDirectory() {
            var resourceDirectory =
                    descriptorFile.toPath()
                                  .getParent();
            protocTask.getSourceSet()
                      .getResources()
                      .srcDir(resourceDirectory);
        }

        private Action<Task> writeRefFile() {
            return task -> {
                var resourceDirectory = descriptorFile.toPath().getParent();
                var reference = DescriptorReference.toOneFile(descriptorFile);
                reference.writeTo(resourceDirectory);
            };
        }

        private void addPlugins() {
            var plugins = protocTask.getPlugins();
            plugins.create(grpc.name());
        }
    }
}
