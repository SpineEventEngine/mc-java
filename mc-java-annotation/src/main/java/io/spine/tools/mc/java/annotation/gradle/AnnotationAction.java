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

package io.spine.tools.mc.java.annotation.gradle;

import io.spine.logging.Logging;
import io.spine.tools.code.SourceSetName;
import io.spine.tools.mc.java.annotation.mark.AnnotatorFactory;
import io.spine.tools.mc.java.annotation.mark.DefaultAnnotatorFactory;
import io.spine.tools.mc.java.annotation.mark.ModuleAnnotator;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.tools.gradle.protobuf.ProtobufDependencies.sourceSetExtensionName;
import static io.spine.tools.gradle.protobuf.Projects.descriptorSetFile;
import static io.spine.tools.gradle.protobuf.Projects.protoDirectorySet;
import static io.spine.tools.mc.java.annotation.mark.ApiOption.beta;
import static io.spine.tools.mc.java.annotation.mark.ApiOption.experimental;
import static io.spine.tools.mc.java.annotation.mark.ApiOption.internal;
import static io.spine.tools.mc.java.annotation.mark.ApiOption.spi;
import static io.spine.tools.mc.java.annotation.mark.ModuleAnnotator.translate;
import static io.spine.tools.mc.java.gradle.McJavaOptions.getCodeGenAnnotations;
import static io.spine.tools.mc.java.gradle.McJavaOptions.getInternalClassPatterns;
import static io.spine.tools.mc.java.gradle.McJavaOptions.getInternalMethodNames;
import static io.spine.tools.mc.java.gradle.Projects.generatedGrpcDir;
import static io.spine.tools.mc.java.gradle.Projects.generatedJavaDir;
import static io.spine.tools.proto.fs.Directory.rootName;

/**
 * A task action which annotates the generated code.
 */
final class AnnotationAction implements Action<Task>, Logging {

    private final SourceSetName sourceSetName;

    /**
     * Creates a new action instance.
     */
    AnnotationAction(SourceSetName ssn) {
        this.sourceSetName = checkNotNull(ssn);
    }

    @Override
    public void execute(Task task) {
        var project = task.getProject();
        if (!containsProtoCode(project)) {
            return;
        }
        var descriptorSetFile = descriptorSetFile(project, sourceSetName);
        if (!descriptorSetFile.exists()) {
            logMissing(project.getLogger(), descriptorSetFile);
            return;
        }
        var annotator = createAnnotator(project);
        annotator.annotate();
    }

    /** Verifies of the source set of the given project contains Protobuf source code. */
    private boolean containsProtoCode(Project project) {
        var protoSet = protoDirectorySet(project, sourceSetName);
        if (protoSet == null) {
            return false;
        }
        var dirs = protoSet.getSourceDirectories().getFiles();
        var hasProtoDir = dirs.stream()
                .anyMatch(dir -> dir.getPath().endsWith(rootName()));
        var isEmpty =  protoSet.isEmpty();
        return hasProtoDir && !isEmpty;
    }

    private ModuleAnnotator createAnnotator(Project project) {
        var annotatorFactory = createAnnotationFactory(project);
        var annotations = getCodeGenAnnotations(project);
        var internalClassName = annotations.internalClassName();
        var internalClassPatterns = getInternalClassPatterns(project);
        var internalMethodNames = getInternalMethodNames(project);
        return ModuleAnnotator.newBuilder()
                .setAnnotatorFactory(annotatorFactory)
                .add(translate(spi()).as(annotations.spiClassName()))
                .add(translate(beta()).as(annotations.betaClassName()))
                .add(translate(experimental()).as(annotations.experimentalClassName()))
                .add(translate(internal()).as(internalClassName))
                .setInternalPatterns(internalClassPatterns)
                .setInternalMethodNames(internalMethodNames)
                .setInternalAnnotation(internalClassName)
                .build();
    }

    private AnnotatorFactory createAnnotationFactory(Project project) {
        var ssn = sourceSetName;
        var descriptorSetFile = descriptorSetFile(project, ssn);
        var generatedJavaPath = generatedJavaDir(project, ssn);
        var generatedGrpcPath = generatedGrpcDir(project, ssn);
        var annotatorFactory = DefaultAnnotatorFactory.newInstance(
                descriptorSetFile, generatedJavaPath, generatedGrpcPath
        );
        return annotatorFactory;
    }

    private void logMissing(Logger logger, File descriptorSetFile) {
        var nl = System.lineSeparator();
        logger.warn(
                "Missing descriptor set file `{}`" +
                        " produced for the source set `{}` which has `{}` extension." + nl +
                        "Please enable descriptor set generation." + nl +
                        "See: " +
                        "https://github.com/google/protobuf-gradle-plugin/blob/master/README.md" +
                        "#generate-descriptor-set-files",
                descriptorSetFile.getPath(),
                sourceSetName,
                sourceSetExtensionName
        );
    }
}
