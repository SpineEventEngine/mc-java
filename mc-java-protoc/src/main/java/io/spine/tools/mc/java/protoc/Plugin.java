/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.tools.mc.java.protoc;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import io.spine.option.OptionsProvider;
import io.spine.tools.mc.java.protoc.message.BuilderGen;
import io.spine.tools.mc.java.protoc.message.InterfaceGen;
import io.spine.tools.mc.java.protoc.message.NestedClassGen;
import io.spine.tools.mc.java.protoc.method.MethodGen;
import io.spine.tools.mc.java.settings.CodegenSettings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.string.Strings.decodeBase64;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A Protobuf Compiler ({@literal a.k.a.} {@code protoc}) plugin.
 *
 * <p>The program reads
 * a {@link com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest CodeGeneratorRequest}
 * from {@code System.in} and writes
 * a {@link com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse CodeGeneratorResponse}
 * into the {@code System.out}.
 *
 * <p>For the description of the plugin behavior see
 * {@link io.spine.tools.mc.java.protoc.message.InterfaceGen InterfaceGen} and
 * {@link io.spine.tools.mc.java.protoc.method.MethodGen MethodGen}.
 *
 * <p>For the plugin mechanism see <a href="SpineProtoGenerator.html#contract">
 * {@code SpineProtoGenerator}</a>.
 */
public final class Plugin {

    /** Prevents instantiation from the outside. */
    private Plugin() {
    }

    /**
     * The entry point of the program.
     */
    public static void main(String[] args) {
        var request = readRequest();
        var config = readConfig(request);
        var generator = CompositeGenerator.of(
                InterfaceGen.instance(config),
                MethodGen.instance(config),
                new BuilderGen(),
                NestedClassGen.instance(config)
                //EntityQueryGen.instance(config)
                //FieldGen.instance(config)
        );
        var response = generator.process(request);
        writeResponse(response);
    }

    private static ExtensionRegistry registry() {
        return OptionsProvider.registryWithAllOptions();
    }

    private static CodeGeneratorRequest readRequest() {
        try {
            var request = CodeGeneratorRequest.parseFrom(System.in, registry());
            return request;
        } catch (IOException e) {
            throw newIllegalStateException(e, "Unable to read Code Generator Request.");
        }
    }

    private static CodegenSettings readConfig(CodeGeneratorRequest request) {
        var configFilePath = decodeBase64(request.getParameter());
        try (var fis = new FileInputStream(configFilePath)) {
            var config = CodegenSettings.parseFrom(fis, registry());
            return config;
        } catch (InvalidProtocolBufferException e) {
            throw newIllegalStateException(e, "Unable to decode Spine Protoc Plugin config.");
        } catch (FileNotFoundException e) {
            throw newIllegalStateException(e, "Spine Protoc Plugin config file not found.");
        } catch (IOException e) {
            throw newIllegalStateException(e, "Unable to read Spine Protoc Plugin config.");
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr") // Required by the protoc API.
    private static void writeResponse(CodeGeneratorResponse response) {
        checkNotNull(response);
        try {
            response.writeTo(System.out);
        } catch (IOException e) {
            throw newIllegalStateException(
                    e, "Unable to write Spine Protoc Plugin code generator response.");
        }
    }
}
