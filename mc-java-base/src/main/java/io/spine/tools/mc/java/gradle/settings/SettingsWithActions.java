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

package io.spine.tools.mc.java.gradle.settings;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.tools.gradle.Multiple;
import io.spine.tools.gradle.Ordered;
import io.spine.tools.mc.java.settings.AddInterface;
import org.checkerframework.checker.signature.qual.FqBinaryName;
import org.gradle.api.Project;
import org.gradle.api.provider.SetProperty;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.tools.java.code.Names.className;

/**
 * Code generation settings that cover applying code generation actions specified as
 * fully qualified binary names of classes that extend {@link io.spine.tools.mc.java.MessageAction}.
 *
 * @param <P>
 *         Protobuf type reflecting a snapshot of these settings
 */
public abstract class SettingsWithActions<P extends Message> extends Settings<P> {

    @Deprecated
    private final Multiple<String> interfaceNames;

    private final Ordered<@FqBinaryName String> actions;

    /**
     * Creates an instance of settings for the specified project, configuring the convention
     * using the passed default values.
     *
     * @param p
     *        the project for which settings are created
     * @param defaultActions
     *         actions to be specified as the default value for the settings
     */
    SettingsWithActions(Project p, Iterable<@FqBinaryName String> defaultActions) {
        super(p);
        checkNotNull(defaultActions);
        this.interfaceNames = new Multiple<>(p, String.class);
        this.actions = new Ordered<>(p, String.class);
        actions.convention(defaultActions);
    }

    /**
     * Configures the associated messages to implement a Java interface with the given name.
     *
     * <p>The declaration of the interface in Java must exist. It will not be generated. Providing
     * a non-existent interface may lead to a compiler error.
     *
     * @param interfaceName
     *         the canonical name of the interface
     * @deprecated Please call {@link #useAction(String)} with corresponding codegen action
     *         class name instead.
     */
    @Deprecated
    public final void markAs(String interfaceName) {
        interfaceNames.add(interfaceName);
    }

    /**
     * Instructs Model Compiler to use
     * the {@linkplain io.spine.protodata.renderer.RenderAction code generation action}
     * specified by the binary name of the class.
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
     * {@linkplain io.spine.protodata.renderer.RenderAction code generation actions}
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
     * {@linkplain io.spine.protodata.renderer.RenderAction code generation actions}
     * to the code generated for messages of this group.
     *
     * @param classNames
     *         the binary names of the action classes
     */
    public void useActions(@FqBinaryName String... classNames) {
        useActions(ImmutableList.copyOf(classNames));
    }

    /**
     * Obtains currently assigned codegen actions.
     */
    protected final Iterable<@FqBinaryName String> actions() {
        return actions.getOrElse(ImmutableList.of());
    }

    /**
     * Obtains the set of {@link AddInterface}s which define which interfaces to add
     * to the associated messages.
     *
     * @deprecated Please use {@link #actions()} instead.
     */
    @Deprecated
    final ImmutableSet<AddInterface> interfaces() {
        return interfaceNames.transform(name -> AddInterface.newBuilder()
                .setName(className(name))
                .build());
    }

    /**
     * Obtains the Gradle property containing the set of Java interface names.
     *
     * @deprecated Please use {@link #actions()} instead.
     */
    @Deprecated
    final SetProperty<String> interfaceNames() {
        return interfaceNames;
    }
}
