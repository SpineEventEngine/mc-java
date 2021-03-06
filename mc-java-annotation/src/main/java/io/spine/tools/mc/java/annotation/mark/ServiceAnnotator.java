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
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import io.spine.tools.java.fs.SourceFile;
import io.spine.code.java.ClassName;

import java.nio.file.Path;

/**
 * An annotator for {@code gRPC} services.
 *
 * <p>Annotates a service in a generated Java source
 * if a specified {@linkplain com.google.protobuf.DescriptorProtos.ServiceOptions service option}
 * value is {@code true} for a service definition.
 */
final class ServiceAnnotator extends OptionAnnotator<ServiceDescriptor> {

    ServiceAnnotator(ClassName annotation,
                     ApiOption option,
                     ImmutableList<FileDescriptor> fileDescriptors,
                     Path genProtoDir) {
        super(annotation, option, fileDescriptors, genProtoDir);
    }

    @Override
    public void annotate() {
        for (var fileDescriptor : fileDescriptors()) {
            annotate(fileDescriptor);
        }
    }

    @Override
    protected void annotateOneFile(FileDescriptor fileDescriptor) {
        annotateServices(fileDescriptor);
    }

    @Override
    protected void annotateMultipleFiles(FileDescriptor file) {
        annotateServices(file);
    }

    private void annotateServices(FileDescriptor file) {
        for (var service : file.getServices()) {
            if (shouldAnnotate(service)) {
                var serviceClass = SourceFile.forService(service.toProto(), file.toProto());
                annotate(serviceClass);
            }
        }
    }

    @Override
    protected boolean shouldAnnotate(ServiceDescriptor descriptor) {
        return option().isPresentAt(descriptor);
    }
}
