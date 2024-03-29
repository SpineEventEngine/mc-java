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

package io.spine.tools.mc.java.gradle.codegen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.tools.gradle.Multiple;
import io.spine.tools.mc.java.codegen.GenerateMethods;
import io.spine.tools.mc.java.codegen.GenerateNestedClasses;
import io.spine.tools.mc.java.codegen.Messages;
import io.spine.tools.mc.java.codegen.MethodFactoryName;
import io.spine.tools.mc.java.codegen.NestedClassFactoryName;
import io.spine.tools.mc.java.codegen.Pattern;
import org.gradle.api.Project;

import java.util.Set;

import static io.spine.protobuf.Messages.isNotDefault;
import static io.spine.tools.java.code.Names.className;
import static java.util.stream.Collectors.toSet;

/**
 * A codegen configuration for messages which match a certain pattern.
 *
 * @see CodegenOptionsConfig#forMessages
 */
public final class MessagesConfig extends ConfigWithFields<Messages> {

    private final Pattern pattern;
    private final Multiple<String> methodFactories;
    private final Multiple<String> nestedClassFactories;

    MessagesConfig(Project p, Pattern pattern) {
        super(p);
        this.pattern = pattern;
        methodFactories = new Multiple<>(p, String.class);
        methodFactories.convention(ImmutableList.of());
        nestedClassFactories = new Multiple<>(p, String.class);
        nestedClassFactories.convention(ImmutableList.of());
    }

    void emptyByConvention() {
        interfaceNames().convention(ImmutableSet.of());
        methodFactories.convention(ImmutableSet.of());
        nestedClassFactories.convention(ImmutableSet.of());
    }

    /**
     * Specifies a {@link io.spine.tools.java.code.MethodFactory MethodFactory} to generate
     * methods for the message classes.
     *
     * <p>Calling this method multiple times will add the provided factories for code generation.
     *
     * @param factoryClassName
     *         the canonical class name of the method factory
     */
    public void generateMethodsWith(String factoryClassName) {
        methodFactories.add(factoryClassName);
    }

    /**
     * Specifies a {@link io.spine.tools.java.code.NestedClassFactory NestedClassFactory}
     * to generate nested classes inside the message classes.
     *
     * <p>Calling this method multiple times will add the provided factories for code generation.
     *
     * @param factoryClassName
     *         the canonical class name of the nested class factory
     */
    public void generateNestedClassesWith(String factoryClassName) {
        nestedClassFactories.add(factoryClassName);
    }

    @Override
    public Messages toProto() {
        var result = Messages.newBuilder()
                .setPattern(pattern)
                .addAllAddInterface(interfaces())
                .addAllGenerateMethods(generateMethods())
                .addAllGenerateNestedClasses(generateNestedClasses());
        var generateFields = generateFields();
        if (isNotDefault(generateFields)) {
            result.setGenerateFields(generateFields);
        }
        return result.build();
    }

    private Set<GenerateMethods> generateMethods() {
        return methodFactories.get()
                              .stream()
                              .map(MessagesConfig::methodFactoryConfig)
                              .collect(toSet());
    }

    private static GenerateMethods methodFactoryConfig(String methodFactoryClass) {
        var factoryName = MethodFactoryName.newBuilder()
                .setClassName(className(methodFactoryClass))
                .build();
        var config = GenerateMethods.newBuilder()
                .setFactory(factoryName)
                .build();
        return config;
    }

    private Set<GenerateNestedClasses> generateNestedClasses() {
        return nestedClassFactories.get()
                                   .stream()
                                   .map(MessagesConfig::nestedClassFactoryConfig)
                                   .collect(toSet());
    }

    private static GenerateNestedClasses nestedClassFactoryConfig(String methodFactoryClass) {
        var factoryName = NestedClassFactoryName.newBuilder()
                .setClassName(className(methodFactoryClass))
                .build();
        var config = GenerateNestedClasses.newBuilder()
                .setFactory(factoryName)
                .build();
        return config;
    }
}
