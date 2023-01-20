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

import com.google.common.collect.ImmutableSet;
import io.spine.base.RejectionThrowable;
import io.spine.code.java.PackageName;
import io.spine.code.java.SimpleClassName;
import io.spine.code.proto.FileSet;
import io.spine.code.proto.SourceFile;
import io.spine.tools.code.Indent;
import io.spine.tools.gradle.CodeGenerationAction;
import io.spine.tools.gradle.ProtoFiles;
import io.spine.tools.code.SourceSetName;
import io.spine.tools.java.code.TypeSpec;
import io.spine.tools.java.code.TypeSpecWriter;
import io.spine.tools.mc.java.gradle.McJavaOptions;
import io.spine.tools.mc.java.gradle.Projects;
import io.spine.tools.mc.java.rejection.gen.RThrowableSpec;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.flogger.LazyArgs.lazy;
import static io.spine.tools.gradle.project.Projects.getSourceSets;
import static io.spine.tools.mc.java.gradle.Projects.generatedRejectionsDir;
import static io.spine.tools.mc.java.gradle.Projects.protoDir;

/**
 * Generates source code of rejections.
 *
 * <p>For each message type declared in the {@code rejections.proto} generates a corresponding
 * rejection type which extends {@link RejectionThrowable RejectionThrowable} and
 * encloses an instance of the corresponding proto message.
 *
 * <p>The {@link McJavaOptions#indent} option sets the indentation of the generated source files.
 */
final class RejectionGenAction extends CodeGenerationAction {

    private final SourceSetName ssn;

    private RejectionGenAction(Project project,
                               SourceSetName ssn,
                               Supplier<String> protoSrcDir,
                               Supplier<FileSet> protoFiles,
                               Supplier<String> targetDir) {
        super(project, protoFiles, targetDir, protoSrcDir);
        this.ssn = checkNotNull(ssn);
    }

    /**
     * Creates an action for generating Java source code for rejection types defined in proto
     * files in the given sources set of the project.
     */
    static Action<Task> create(Project project, SourceSetName ssn) {
        Supplier<String> protoSrcDir = () -> protoDir(project, ssn).toString();
        var protoFiles = ProtoFiles.collect(project, ssn);
        Supplier<String> targetDir = () -> generatedRejectionsDir(project, ssn).toString();

        extendSourceSet(project, ssn, targetDir);

        return new RejectionGenAction(project, ssn, protoSrcDir, protoFiles, targetDir);
    }

    /**
     * Adds the given {@code targetDir} to the source set with the given name.
     */
    private static void extendSourceSet(Project project,
                                        SourceSetName ssn,
                                        Supplier<String> targetDir) {
        var sourceSets = getSourceSets(project);
        var sourceSet = sourceSets.getByName(ssn.getValue());
        var dir = project.provider(targetDir::get);
        sourceSet.java(sds -> sds.srcDir(dir));
    }

    @Override
    public void execute(Task task) {
        var files = protoFiles().get();
        var rejectionFiles = rejectionsInSourceSet(files);
        _debug().log("Processing the file descriptors for the rejections `%s`.", rejectionFiles);
        for (var source : rejectionFiles) {
            // We are sure that this is a rejections file because we got them filtered.
            generateRejections(source);
        }
    }

    /**
     * Obtains all rejection files in the currently processed {@linkplain #ssn source set}.
     */
    private ImmutableSet<RejectionsFile> rejectionsInSourceSet(FileSet allFiles) {
        var allRejections = RejectionsFile.findAll(allFiles);
        var inSourceSet = belongsToSourceSet();
        var moduleRejections = allRejections.stream()
                .filter(inSourceSet)
                .collect(toImmutableSet());
        return moduleRejections;
    }

    /**
     * Obtains the predicate which accepts sources files belonging to currently served
     * {@linkplain #ssn source set}.
     */
    private Predicate<SourceFile> belongsToSourceSet() {
        @Nullable FileCollection fileCollection = Projects.protoFiles(project(), ssn);
        checkState(fileCollection != null, "No proto files found in the source set `%s`.", ssn);
        Set<Path> protoFiles = fileCollection.getFiles()
                .stream()
                .map(File::toPath)
                .collect(toImmutableSet());
        return file -> {
            var sourceFile = file.path();
            return protoFiles.stream().anyMatch(path -> path.endsWith(sourceFile));
        };
    }

    private void generateRejections(RejectionsFile source) {
        var rejections = source.rejectionDeclarations();
        if (rejections.isEmpty()) {
            return;
        }
        var outputDir = targetDir().toPath();
        logGeneratingForFile(outputDir, source);
        for (var rejectionType : rejections) {
            // The name of the generated `ThrowableMessage` will be the same
            // as for the Protobuf message.
            _debug().log("Processing rejection `%s`.", rejectionType.simpleJavaClassName());

            TypeSpec spec = new RThrowableSpec(rejectionType);
            var writer = new TypeSpecWriter(spec, indent());
            writer.write(outputDir);
        }
    }

    private void logGeneratingForFile(Path outputDir, RejectionsFile source) {
        _debug().log(
                "Generating rejections from the file: `%s`" +
                        " `javaPackage`: `%s`," +
                        " `javaOuterClassName`: `%s`." +
                        " Output directory: `%s`.",
                source.path(),
                lazy(() -> PackageName.resolve(source.descriptor().toProto())),
                lazy(() -> SimpleClassName.outerOf(source.descriptor())),
                outputDir
        );
    }

    @Override
    protected Indent getIndent(Project project) {
        return McJavaOptions.getIndent(project);
    }
}
