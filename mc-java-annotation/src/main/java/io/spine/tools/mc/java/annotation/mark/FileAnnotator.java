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

package io.spine.tools.mc.java.annotation.mark;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.Descriptors.FileDescriptor;
import io.spine.code.java.ClassName;
import io.spine.tools.java.fs.SourceFile;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.nio.file.Path;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.tools.java.fs.SourceFile.forEnum;
import static io.spine.tools.java.fs.SourceFile.forService;

/**
 * A file-level annotator.
 *
 * <p>Annotates generated top-level definitions from a {@code .proto} file,
 * if a specified {@linkplain FileOptions file option} value is {@code true}.
 */
final class FileAnnotator extends OptionAnnotator<FileDescriptor> {

    private final Path genGrpcDir;

    FileAnnotator(ClassName annotation,
                  ApiOption option,
                  ImmutableList<FileDescriptor> files,
                  Path genProtoDir,
                  Path genGrpcDir) {
        super(annotation, option, files, genProtoDir);
        checkNotNull(genGrpcDir);
        this.genGrpcDir = genGrpcDir;
    }

    @Override
    public void annotate() {
        for (var file : fileDescriptors()) {
            if (shouldAnnotate(file)) {
                annotate(file);
            }
        }
    }

    @Override
    protected void annotateOneFile(FileDescriptor file) {
        annotateServices(file);
        annotateNestedTypes(file);
    }

    @Override
    protected void annotateMultipleFiles(FileDescriptor file) {
        annotateMessages(file);
        annotateEnums(file);
        annotateServices(file);
    }

    @Override
    protected boolean shouldAnnotate(FileDescriptor descriptor) {
        return option().isPresentAt(descriptor);
    }

    /**
     * Annotates all nested types in a generated
     * {@linkplain FileOptions#getJavaOuterClassname() outer class}.
     *
     * @param file the file descriptor to get the outer class.
     * @see #annotateServices(FileDescriptor)
     */
    private void annotateNestedTypes(FileDescriptor file) {
        var filePath = SourceFile.forOuterClassOf(file.toProto());
        rewriteSource(filePath, input -> input.getNestedTypes().forEach(this::addAnnotation));
    }

    /**
     * Annotates all messages generated from the specified {@link FileDescriptor}.
     *
     * <p>The specified file descriptor should
     * {@linkplain FileOptions#getJavaMultipleFiles() have multiple Java files}.
     *
     * @param file the file descriptor to get message descriptors
     */
    private void annotateMessages(FileDescriptor file) {
        for (var messageType : file.getMessageTypes()) {
            annotateMessageTypes(messageType, file);
        }
    }

    /**
     * Annotates all enums generated from the specified {@link FileDescriptor}.
     *
     * <p>The specified file descriptor should
     * {@linkplain FileOptions#getJavaMultipleFiles() have multiple Java files}.
     *
     * @param file the file descriptor to get enum descriptors
     */
    private void annotateEnums(FileDescriptor file) {
        for (var enumType : file.getEnumTypes()) {
            var filePath = forEnum(enumType.toProto(), file.toProto());
            annotate(filePath);
        }
    }

    /**
     * Annotates all {@code gRPC} services,
     * generated basing on the specified {@link FileDescriptor}.
     *
     * <p>A generated service is always a separate file.
     * So value of {@link FileOptions#getJavaMultipleFiles()} does not play a role.
     *
     * @param file the file descriptor to get service descriptors
     */
    private void annotateServices(FileDescriptor file) {
        SourceVisitor<JavaClassSource> annotation = new TypeDeclarationAnnotation();
        for (var serviceDescriptor : file.getServices()) {
            var serviceClass = forService(serviceDescriptor.toProto(), file.toProto());
            rewriteSource(genGrpcDir, serviceClass, annotation);
        }
    }
}
