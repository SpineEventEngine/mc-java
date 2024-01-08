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
import com.google.protobuf.gradle.ProtobufExtension;
import io.spine.tools.gradle.DependencyVersions;
import io.spine.tools.mc.gradle.LanguagePlugin;
import io.spine.tools.mc.java.checks.gradle.McJavaChecksPlugin;
import io.spine.tools.mc.java.gradle.McJavaOptions;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import io.spine.tools.mc.java.gradle.McJava;

import java.util.stream.Stream;

import static io.spine.tools.gradle.Artifact.PLUGIN_BASE_ID;
import static io.spine.tools.gradle.protobuf.ProtobufDependencies.protobufCompiler;
import static io.spine.tools.mc.java.gradle.Projects.getMcJava;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

/**
 * Spine Model Compiler for Java Gradle plugin.
 *
 * <p>Applies dependent plugins.
 */
public class McJavaPlugin extends LanguagePlugin {

    public McJavaPlugin() {
        super(McJavaOptions.name(), getKotlinClass(McJavaOptions.class));
    }

    @Override
    public void apply(Project project) {
        super.apply(project);
        logApplyingTo(project);
        setProtocArtifact(project);
        var extension = getMcJava(project);
        extension.injectProject(project);
        createAndApplyPluginsIn(project);
    }

    private void logApplyingTo(Project project) {
        var version = McJava.version();
        project.getLogger().warn(
                "Applying `{}` (version: `{}`) to `{}`.",
                getClass().getName(),
                version,
                project.getName()
        );
    }

    private static void setProtocArtifact(Project project) {
        var protobuf = project.getExtensions().getByType(ProtobufExtension.class);
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
    private static void createAndApplyPluginsIn(Project project) {
        Stream.of(new CleaningPlugin(),
                  new DescriptorSetMergerPlugin(),
                  new JavaProtocConfigurationPlugin(),
                  new McJavaChecksPlugin(),
                  new ProtoDataConfigPlugin())
              .forEach(plugin -> apply(plugin, project));
    }

    private static void apply(Plugin<Project> plugin, Project project) {
        project.getLogger()
               .debug(
                       "Applying plugin `{}` to project `{}`.",
                       plugin.getClass().getName(), project.getName()
               );
        plugin.apply(project);
    }
}
