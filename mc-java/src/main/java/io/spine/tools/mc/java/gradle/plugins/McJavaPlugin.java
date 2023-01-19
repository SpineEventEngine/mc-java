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

import com.google.protobuf.gradle.ExecutableLocator;
import io.spine.logging.Logging;
import io.spine.tools.gradle.DependencyVersions;
import io.spine.tools.mc.gradle.LanguagePlugin;
import io.spine.tools.mc.java.annotation.gradle.AnnotatorPlugin;
import io.spine.tools.mc.java.checks.gradle.McJavaChecksPlugin;
import io.spine.tools.mc.java.gradle.McJavaOptions;
import io.spine.tools.mc.java.rejection.gradle.RejectionGenPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.stream.Stream;

import static io.spine.tools.gradle.Artifact.PLUGIN_BASE_ID;
import static io.spine.tools.gradle.protobuf.ProtobufDependencies.protobufCompiler;
import static io.spine.tools.gradle.protobuf.ProtobufGradlePluginAdapterKt.getProtobufGradlePluginAdapter;
import static io.spine.tools.mc.java.gradle.Projects.getMcJava;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

/**
 * Spine Model Compiler for Java Gradle plugin.
 *
 * <p>Applies dependent plugins.
 */
public class McJavaPlugin extends LanguagePlugin implements Logging {

    public McJavaPlugin() {
        super(McJavaOptions.name(), getKotlinClass(McJavaOptions.class));
    }

    @Override
    public void apply(Project project) {
        super.apply(project);
        setProtocArtifact(project);
        var extension = getMcJava(project);
        extension.injectProject(project);
        createAndApplyPluginsIn(project);
    }

    private static void setProtocArtifact(Project project) {
        var protobuf = getProtobufGradlePluginAdapter(project);
        var ofPluginBase = DependencyVersions.loadFor(PLUGIN_BASE_ID);
        var protocArtifact = protobufCompiler.withVersionFrom(ofPluginBase).notation();
        protobuf.protoc((ExecutableLocator locator) -> locator.setArtifact(protocArtifact));
    }
    /**
     * Creates all the plugins that are parts of {@code mc-java} and applies them to
     * the given project.
     *
     * @implNote Plugins that deal with Protobuf types must depend on
     *         {@code mergeDescriptorSet} and {@code mergeTestDescriptorSet} tasks to be able to
     *         access every declared type in the project classpath.
     */
    private void createAndApplyPluginsIn(Project project) {
        Stream.of(new CleaningPlugin(),
                  new DescriptorSetMergerPlugin(),
                  new RejectionGenPlugin(),
                  new AnnotatorPlugin(),
                  new JavaProtocConfigurationPlugin(),
                  new McJavaChecksPlugin(),
                  new ProtoDataConfigPlugin())
              .forEach(plugin -> apply(plugin, project));
    }

    private void apply(Plugin<Project> plugin, Project project) {
        _debug().log("Applying plugin `%s` to project `%s`.",
                     plugin.getClass().getName(), project.getName());
        plugin.apply(project);
    }
}
