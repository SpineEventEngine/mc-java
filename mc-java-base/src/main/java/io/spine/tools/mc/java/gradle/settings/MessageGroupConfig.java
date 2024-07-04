/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.gradle.settings;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.tools.gradle.Multiple;
import io.spine.tools.mc.java.settings.GenerateMethods;
import io.spine.tools.mc.java.settings.MessageGroup;
import io.spine.tools.mc.java.settings.MethodFactoryName;
import io.spine.tools.mc.java.settings.Pattern;
import org.checkerframework.checker.signature.qual.FqBinaryName;
import org.gradle.api.Project;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.Messages.isNotDefault;
import static io.spine.tools.java.code.Names.className;
import static java.util.stream.Collectors.toSet;

/**
 * A codegen configuration for messages which match a certain pattern.
 *
 * @see CodegenConfig#forMessages
 */
public final class MessageGroupConfig extends ConfigWithFields<MessageGroup> {

    private final Pattern pattern;
    private final Multiple<String> methodFactories;
    private final Multiple<String> nestedClassFactories;
    private final Multiple<String> actions;

    MessageGroupConfig(Project p, Pattern pattern) {
        super(p);
        this.pattern = pattern;
        methodFactories = new Multiple<>(p, String.class);
        nestedClassFactories = new Multiple<>(p, String.class);
        actions = new Multiple<>(p, String.class);
        emptyByConvention();
    }

    private void emptyByConvention() {
        interfaceNames().convention(ImmutableSet.of());
        methodFactories.convention(ImmutableSet.of());
        nestedClassFactories.convention(ImmutableSet.of());
        actions.convention(ImmutableSet.of());
    }

    /**
     * Instructs Model Compiler to use
     * the {@link io.spine.protodata.renderer.RenderAction code generation action} specified
     * by the binary name of the class.
     *
     * @param className
     *         the binary name of the action class
     */
    public void useAction(@FqBinaryName String className) {
        checkNotNull(className);
        actions.add(className);
    }

    /**
     * Instructs Model Compiler to apply
     * {@link io.spine.protodata.renderer.RenderAction code generation actions}
     * to the code generated for messages of this group.
     *
     * @param classNames
     *         the binary names of the action class
     */
    public void useActions(Iterable<@FqBinaryName String> classNames) {
        checkNotNull(classNames);
        actions.addAll(classNames);
    }

    /**
     * Instructs Model Compiler to apply
     * {@link io.spine.protodata.renderer.RenderAction code generation actions}
     * to the code generated for messages of this group.
     *
     * @param classNames
     *         the binary names of the action classes
     */
    public void useActions(@FqBinaryName String... classNames) {
        useActions(ImmutableList.copyOf(classNames));
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
     * @deprecated please use {@link #useAction} instead.
     */
    @Deprecated
    public void generateNestedClassesWith(String factoryClassName) {
        nestedClassFactories.add(factoryClassName);
    }

    @Override
    public MessageGroup toProto() {
        var result = MessageGroup.newBuilder()
                .setPattern(pattern)
                .addAllAddInterface(interfaces())
                .addAllGenerateMethods(generateMethods())
                .addAllAction(actions.get());
        var generateFields = generateFields();
        if (isNotDefault(generateFields)) {
            result.setGenerateFields(generateFields);
        }
        return result.build();
    }

    private Set<GenerateMethods> generateMethods() {
        return methodFactories.get()
                              .stream()
                              .map(MessageGroupConfig::methodFactoryConfig)
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
}
