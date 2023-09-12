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

import io.spine.tools.mc.java.codegen.CodegenOptions;
import io.spine.tools.proto.code.ProtoOption;
import io.spine.validation.MessageMarkers;
import io.spine.validation.ValidationConfig;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

import static io.spine.tools.mc.java.gradle.Projects.getMcJava;
import static java.nio.file.Files.write;
import static java.util.stream.Collectors.toSet;

/**
 * A task that writes the ProtoData configuration into a file.
 *
 * <p>The {@link #getTargetFile() targetFile} property defines the destination file.
 *
 * <p>This task configures ProtoData-based validation codegen. It tells which files and types
 * are considered entities and signals, so that the Validation library may add extra constraints
 * for those types.
 */
@SuppressWarnings({"AbstractClassNeverImplemented", "unused"})
    // Gradle creates a subtype for this class.
public abstract class GenerateProtoDataConfig extends DefaultTask {

    /**
     * The file where the config is written.
     *
     * <p>It's recommended to put this file under the {@code build} directory of the associated
     * project, so that it is deleted upon {@code clean}-ing the project.
     */
    @OutputFile
    public abstract RegularFileProperty getTargetFile();

    @TaskAction
    private void writeFile() throws IOException {
        var options = getMcJava(getProject());
        var codegen = options.codegen.toProto();
        var makers = MessageMarkers.newBuilder()
                .addAllCommandPattern(codegen.getCommands().getPatternList())
                .addAllEventPattern(codegen.getEvents().getPatternList())
                .addAllRejectionPattern(codegen.getRejections().getPatternList())
                .addAllEntityPattern(codegen.getEntities().getPatternList())
                .addAllEntityOptionName(entityOptionsNames(codegen))
                .build();
        var config = ValidationConfig.newBuilder()
                .setMessageMarkers(makers)
                .build();
        var file = getProject().file(getTargetFile());
        file.getParentFile().mkdirs();
        write(file.toPath(), config.toByteArray());
    }

    private static Iterable<String> entityOptionsNames(CodegenOptions codegen) {
        return codegen.getEntities()
                      .getOptionList()
                      .stream()
                      .map(ProtoOption::getName)
                      .collect(toSet());
    }
}
