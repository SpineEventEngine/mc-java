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
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.GenericDescriptor;
import io.spine.code.java.ClassName;
import io.spine.tools.java.fs.SourceFile;

import java.nio.file.Path;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link Annotator} which annotates Java sources basing on a given Protobuf option.
 *
 * <p>Subtypes are purposed for different Protobuf option types such as {@code FileOptions},
 * {@code MessageOptions} etc.
 *
 * <p>Depending on the option type, an annotator manages a corresponding Protobuf descriptor
 * (e.g. {@code FileDescriptorProto} for {@code FileOptions}).
 *
 * @param <D>
 *         the proto descriptor type used to receive {@link #option} value
 */
public abstract class OptionAnnotator<D extends GenericDescriptor> extends Annotator {

    /**
     * An Protobuf option, that tells whether generated program elements should be annotated.
     *
     * <p>Can be of any option type, which is {@code boolean}.
     */
    private final ApiOption option;

    protected OptionAnnotator(ClassName annotation,
                              ApiOption option,
                              ImmutableList<FileDescriptor> fileDescriptors,
                              Path genProtoDir) {
        super(annotation, fileDescriptors, genProtoDir);
        this.option = checkNotNull(option);
    }

    /**
     * Annotates the Java sources generated from the specified file descriptor.
     */
    protected final void annotate(FileDescriptor fileDescriptor) {
        var multipleFiles = fileDescriptor.getOptions().getJavaMultipleFiles();
        if (multipleFiles) {
            annotateMultipleFiles(fileDescriptor);
        } else {
            annotateOneFile(fileDescriptor);
        }
    }

    /**
     * Annotates the Java sources generated from the specified file descriptor
     * if {@code java_multiple_files} proto file option is set to {@code false}.
     *
     * @param fileDescriptor
     *         the file descriptor
     */
    protected abstract void annotateOneFile(FileDescriptor fileDescriptor);

    /**
     * Annotates the Java sources generated from the specified file descriptor
     * if {@code java_multiple_files} proto file option is {@code true}.
     *
     * @param file
     *         the descriptor of the file
     */
    protected abstract void annotateMultipleFiles(FileDescriptor file);

    /**
     * Tells whether the generated program elements
     * from the specified descriptor should be annotated.
     *
     * @param descriptor
     *         the descriptor to extract {@link #option} value.
     * @return {@code true} if generated element should be annotated, {@code false} otherwise
     */
    protected abstract boolean shouldAnnotate(D descriptor);

    /** Obtains the Protobuf API option to transform into a Java annotation. */
    protected final ApiOption option() {
        return option;
    }

    /**
     * Annotates message class and MessageOrBuilder interface that correspond to the passed type.
     */
    protected final void annotateMessageTypes(Descriptor type, FileDescriptor file) {
        var typeProto = type.toProto();
        var fileProto = file.toProto();
        annotateMessage(typeProto, fileProto);
        annotateInterface(typeProto, fileProto);
    }

    private void annotateMessage(DescriptorProto type, FileDescriptorProto file) {
        var messageClass = SourceFile.forMessage(type, file);
        annotate(messageClass);
    }

    private void annotateInterface(DescriptorProto type, FileDescriptorProto file) {
        var messageOrBuilderInterface = SourceFile.forMessageOrBuilder(type, file);
        annotate(messageOrBuilderInterface);
    }
}
