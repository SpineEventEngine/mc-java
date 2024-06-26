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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.tools.gradle.Multiple;
import io.spine.tools.mc.java.settings.AddInterface;
import org.gradle.api.Project;
import org.gradle.api.provider.SetProperty;

import static io.spine.tools.java.code.Names.className;

/**
 * A config for messages which can implement certain Java interfaces.
 *
 * @param <P>
 *         Protobuf type reflecting a snapshot of this configuration
 */
abstract class ConfigWithInterfaces<P extends Message> extends Config<P> {

    private final Multiple<String> interfaceNames;

    ConfigWithInterfaces(Project p) {
        super(p);
        this.interfaceNames = new Multiple<>(p, String.class);
    }

    /**
     * Configures the associated messages to implement a Java interface with the given name.
     *
     * <p>The declaration of the interface in Java must exist. It will not be generated. Providing
     * a non-existent interface may lead to a compiler error.
     *
     * @param interfaceName
     *         the canonical name of the interface
     */
    public final void markAs(String interfaceName) {
        interfaceNames.add(interfaceName);
    }

    /**
     * Obtains the set of {@link AddInterface}s which define which interfaces to add
     * to the associated messages.
     */
    final ImmutableSet<AddInterface> interfaces() {
        return interfaceNames.transform(name -> AddInterface.newBuilder()
                .setName(className(name))
                .build());
    }

    /**
     * Obtains the Gradle property containing the set of Java interface names.
     */
    final SetProperty<String> interfaceNames() {
        return interfaceNames;
    }
}
