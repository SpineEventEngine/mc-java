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

package io.spine.tools.mc.java.protoc.given;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;
import io.spine.option.OptionsProto;
import io.spine.tools.mc.java.settings.AddInterface;
import io.spine.tools.mc.java.settings.CodegenSettings;
import io.spine.tools.mc.java.settings.GenerateMethods;
import io.spine.tools.mc.java.settings.GenerateNestedClasses;
import io.spine.tools.mc.java.settings.MethodFactoryName;
import io.spine.tools.mc.java.settings.NestedClassFactoryName;
import io.spine.tools.mc.java.settings.Pattern;
import io.spine.protodata.FilePattern;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;

import static com.google.common.base.Charsets.UTF_8;
import static io.spine.tools.java.code.Names.className;

/**
 * A helper class for {@link com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
 * CodeGeneratorRequest}s creation.
 */
public final class CodeGeneratorRequestGiven {

    /** Prevents instantiation of this utility class. */
    private CodeGeneratorRequestGiven() {
    }

    /**
     * Creates a {@link com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest.Builder
     * CodeGeneratorRequest.Builder} instance with the default Protobuf descriptors and
     * Spine options set.
     */
    public static PluginProtos.CodeGeneratorRequest.Builder requestBuilder() {
        return PluginProtos.CodeGeneratorRequest.newBuilder()
                .addProtoFile(descriptorProto())
                .addProtoFile(spineOptionsProto())
                .setCompilerVersion(compilerVersion());
    }

    /**
     * Creates an instance of the latest supported Protobuf version.
     */
    private static PluginProtos.Version compilerVersion() {
        return PluginProtos.Version.newBuilder()
                .setMajor(3)
                .setMajor(6)
                .setPatch(1)
                .setSuffix("")
                .build();
    }

    /**
     * Creates a {@link GenerateNestedClasses} config with the given factory class.
     */
    public static GenerateNestedClasses generateNested(Class<?> cls) {
        return GenerateNestedClasses.newBuilder()
                .setFactory(nestedClassFactory(cls))
                .build();
    }

    /**
     * Creates a {@link NestedClassFactoryName} with the name of the given class.
     */
    private static NestedClassFactoryName nestedClassFactory(Class<?> cls) {
        return NestedClassFactoryName.newBuilder()
                .setClassName(className(cls))
                .build();
    }

    /**
     * Creates a {@link GenerateMethods} config with the given factory class.
     */
    public static GenerateMethods generateMethods(Class<?> factory) {
        var factoryName = methodFactory(factory);
        return GenerateMethods.newBuilder()
                .setFactory(factoryName)
                .build();
    }

    /**
     * Creates a {@link MethodFactoryName} with the name of the given class.
     */
    public static MethodFactoryName methodFactory(Class<?> cls) {
        return MethodFactoryName.newBuilder()
                .setClassName(className(cls))
                .build();
    }

    /**
     * Creates a {@link AddInterface} config with the given interface.
     */
    public static AddInterface addInterface(Class<?> iface) {
        return AddInterface.newBuilder()
                .setName(className(iface))
                .build();
    }

    /**
     * Creates a {@link Pattern} wrapping the given file pattern.
     */
    public static Pattern pattern(FilePattern filePattern) {
        return Pattern.newBuilder()
                .setFile(filePattern)
                .build();
    }

    /**
     * Writes the given Protoc config into the given file and obtains the Protoc plugin argument.
     *
     * @return the path to the serialized config, encoded in Base64
     */
    public static String protocConfig(CodegenSettings config, Path configFile) {
        try (var fos = new FileOutputStream(configFile.toFile())) {
            config.writeTo(fos);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return base64Encoded(configFile.toAbsolutePath().toString());
    }

    private static String base64Encoded(String value) {
        var valueBytes = value.getBytes(UTF_8);
        var result = Base64.getEncoder().encodeToString(valueBytes);
        return result;
    }

    private static DescriptorProtos.FileDescriptorProto spineOptionsProto() {
        return OptionsProto.getDescriptor()
                           .toProto();
    }

    private static DescriptorProtos.FileDescriptorProto descriptorProto() {
        return DescriptorProtos.getDescriptor()
                               .toProto();
    }
}
