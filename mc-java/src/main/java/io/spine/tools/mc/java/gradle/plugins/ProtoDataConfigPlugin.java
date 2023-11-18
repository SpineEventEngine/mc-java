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

import com.google.common.collect.ImmutableList;
import io.spine.protodata.gradle.CodegenSettings;
import io.spine.protodata.gradle.plugin.LaunchProtoData;
import io.spine.tools.gradle.Artifact;
import io.spine.tools.mc.java.gradle.McJava;
import io.spine.tools.mc.java.gradle.Validation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import static io.spine.tools.fs.DirectoryName.kotlin;
import static io.spine.tools.mc.java.gradle.Artifacts.toolBase;
import static io.spine.tools.mc.java.gradle.Projects.getGeneratedGrpcDirName;
import static io.spine.tools.mc.java.gradle.Projects.getGeneratedJavaDirName;
import static io.spine.tools.mc.java.gradle.Projects.getMcJava;
import static java.io.File.separatorChar;
import static java.lang.String.format;
import static java.util.Objects.requireNonNullElseGet;

/**
 * The plugin that configures ProtoData for the associated project.
 *
 * <p>We use ProtoData and the Validation library to generate validation code right inside
 * the Protobuf message classes. This plugin applies the {@code io.spine.protodata} plugin,
 * configures its extension, writes the ProtoData configuration file, and adds the required
 * dependencies to the target project.
 */
final class ProtoDataConfigPlugin implements Plugin<Project> {

    private static final String PROTO_DATA_ID = "io.spine.protodata";
    private static final String CONFIG_SUBDIR = "protodata-config";

    @SuppressWarnings("DuplicateStringLiteralInspection")
        // Could be duplicated in auto-generated Gradle code via script plugins in `buildSrc`.
    private static final String PROTODATA_CONFIGURATION = "protoData";
    private static final String IMPL_CONFIGURATION = "implementation";

    /**
     * Applies the {@code io.spine.protodata} plugin to the project and, if the user needs
     * validation code generation, configures ProtoData to generate Java validation code.
     *
     * <p>ProtoData configuration is a tricky operation because of Gradle's lifecycle.
     * We need to squeeze our configuration before the {@code LaunchProtoData} task is configured.
     * This means adding the {@code afterEvaluate(..)} hook before the ProtoData Gradle plugin
     * is applied to the project.
     */
    @Override
    public void apply(Project target) {
        target.afterEvaluate(ProtoDataConfigPlugin::configureProtoData);
        target.getPluginManager()
              .apply(PROTO_DATA_ID);
    }

    private static void configureProtoData(Project target) {
        configurePlugins(target);

        var tasks = target.getTasks();
        tasks.withType(LaunchProtoData.class, task -> {
            var name = task.getName();
            var taskName = format("writeConfigFor_%s", name);
            var configTask = tasks.create(
                    taskName,
                    GenerateProtoDataConfig.class,
                    t -> linkConfigFile(target, task, t)
            );
            task.dependsOn(configTask);
        });
    }

    private static void addDependency(
            Project project,
            String configurationName,
            Artifact artifact
    ) {
        var dependency = findDependency(project, artifact);
        project.getDependencies()
               .add(configurationName, requireNonNullElseGet(dependency, artifact::notation));
    }

    private static void addDependencies(
            Project project,
            String configurationName,
            Artifact... artifacts
    ) {
        for (var artifact : artifacts) {
            addDependency(project, configurationName, artifact);
        }
    }

    /**
     * Configures ProtoData with plugins, for the given Gradle project.
     */
    private static void configurePlugins(Project project) {
        var codegen = project.getExtensions()
                             .getByType(CodegenSettings.class);

        configureValidationRendering(project, codegen);

        codegen.plugins(
                "io.spine.tools.mc.java.rejection.RejectionPlugin"
        );

        codegen.setSubDirs(ImmutableList.of(
                getGeneratedJavaDirName().value(),
                getGeneratedGrpcDirName().value(),
                kotlin.value()
        ));

        addDependencies(
                project, PROTODATA_CONFIGURATION,
                McJava.base(),
                McJava.annotation(),
                McJava.rejection(),
                toolBase()
        );
    }

    private static void configureValidationRendering(Project project, CodegenSettings codegen) {
        codegen.plugins(
                "io.spine.validation.java.JavaValidationPlugin"
        );
        var version = getMcJava(project).codegen.validation().getVersion().get();
        addDependency(project, PROTODATA_CONFIGURATION, Validation.javaCodegenBundle(version));
        addDependency(project, IMPL_CONFIGURATION, Validation.javaRuntime(version));
    }

    private static
    void linkConfigFile(Project target, LaunchProtoData task, GenerateProtoDataConfig t) {
        var targetFile = t.getTargetFile();
        var fileName = t.getName() + ".bin";
        var defaultFile = target.getLayout()
                                .getBuildDirectory()
                                .file(CONFIG_SUBDIR + separatorChar + fileName);
        targetFile.convention(defaultFile);
        task.getConfigurationFile()
            .set(targetFile);
    }

    private static @Nullable Dependency findDependency(Project project, Artifact artifact) {
        var dependencies = project.getConfigurations().stream()
                .flatMap(c -> c.getDependencies().stream());

        var found = dependencies.filter((d) ->
            d.getGroup() != null
                    && d.getGroup().equals(artifact.group())
                    && d.getName().equals(artifact.name())
        ).findFirst().orElse(null);
        return found;
    }
}
