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
package io.spine.tools.mc.java.gradle.settings

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.protobuf.Any
import com.google.protobuf.Message
import com.google.protobuf.stringValue
import io.spine.protobuf.pack
import io.spine.protodata.settings.Actions
import io.spine.protodata.settings.actions
import io.spine.tools.mc.java.ImplementInterface
import io.spine.tools.mc.java.settings.superInterface
import org.checkerframework.checker.signature.qual.FqBinaryName
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty

/**
 * Code generation settings that cover applying code generation actions specified as
 * fully qualified binary names of classes that extend [io.spine.tools.mc.java.MessageAction].
 *
 * @param S Protobuf type reflecting a snapshot of these settings.
 *
 * @param project The project for which settings are created.
 * @param defaultActions Actions to be specified as the default value for the settings.
 *
 * @constructor Creates an instance of settings for the specified project, configuring
 *  the convention using the passed default values.
 */
public abstract class SettingsWithActions<S : Message>(
    project: Project,
    defaultActions: Iterable<@FqBinaryName String>
) : Settings<S>(project) {

    private val actions: MapProperty<String, Any> =
        project.objects.mapProperty(String::class.java, Any::class.java)

    init {
        actions.putAll(defaultActions.associateWith { Any.getDefaultInstance() })
    }

    /**
     * Configures the associated messages to implement a Java interface with the given name.
     *
     * The declaration of the interface in Java must exist. It will not be generated.
     * Providing a non-existing interface will result in a compile time error.
     *
     * @param interfaceName The name of interface. It could be a simple name if the generated
     *   types are in the same package. Otherwise, please use a fully qualified canonical name.
     */
    public fun markAs(interfaceName: String) {
        useAction(
            ImplementInterface::class.java.name,
            superInterface { name = interfaceName }
        )
    }

    /**
     * Configures the associated messages to implement a Java interface with the given name.
     *
     * The declaration of the interface in Java must exist. It will not be generated.
     * Providing a non-existing interface will result in a compile time error.
     *
     * @param interfaceName The name of interface. It could be a simple name if the generated
     *   types are in the same package. Otherwise, please use a fully qualified canonical name.
     * @param genericParams The arguments for generic parameters, if [interfaceName] accepts them.
     *   Similarly to [interfaceName] they could be simple names or qualified names depending
     *   on the package.
     */
    public fun markAs(interfaceName: String, genericParams: Iterable<String>) {
        useAction(
            ImplementInterface::class.java.name,
            superInterface {
                name = interfaceName
                this@superInterface.genericParameter.addAll(genericParams)
            }
        )
    }

    /**
     * Instructs Model Compiler to use
     * the [code generation action][io.spine.protodata.renderer.RenderAction]
     * specified by the binary name of the class.
     *
     * @param className The binary name of the action class.
     */
    public fun useAction(className: @FqBinaryName String) {
        actions.put(className, Any.getDefaultInstance())
    }

    /**
     * Instructs Model Compiler to use
     * the [code generation action][io.spine.protodata.renderer.RenderAction]
     * specified by the binary name of the class.
     *
     * @param className The binary name of the action class.
     * @param parameter The parameter to be passed to the constructor of the action.
     */
    public fun useAction(className: @FqBinaryName String, parameter: Message) {
        actions.put(className, parameter.pack())
    }

    /**
     * Instructs Model Compiler to use
     * the [code generation action][io.spine.protodata.renderer.RenderAction]
     * specified by the binary name of the class.
     *
     * This is a convenience method for creating an action with
     * a [StringValue][com.google.protobuf.StringValue] parameter.
     *
     * @param className The binary name of the action class.
     * @param parameter The string which would wrapped into
     *   [StringValue][com.google.protobuf.StringValue] and passed
     *   as a parameter to the constructor of the action.
     */
    public fun useAction(className: @FqBinaryName String, parameter: String) {
        val stringValue = stringValue { value = parameter }
        actions.put(className, stringValue.pack())
    }

    /**
     * Instructs Model Compiler to apply
     * [code generation actions][io.spine.protodata.renderer.RenderAction]
     * to the code generated for messages of this group.
     *
     * @param classNames
     * the binary names of the action class
     */
    public fun useActions(classNames: Iterable<String>) {
        Preconditions.checkNotNull(classNames)
        actions.putAll(classNames.associateWith { Any.getDefaultInstance() })
    }

    /**
     * Instructs Model Compiler to apply
     * [code generation actions][io.spine.protodata.renderer.RenderAction]
     * to the code generated for messages of this group.
     *
     * @param classNames
     * the binary names of the action classes
     */
    public fun useActions(vararg classNames: String) {
        useActions(ImmutableList.copyOf<@FqBinaryName String>(classNames))
    }

    /**
     * Obtains currently assigned codegen actions.
     */
    @VisibleForTesting
    public fun actions(): Actions {
        val collected = actions.get()
        check(collected.isNotEmpty()) {
            "Code generation settings instance (`$this`) does not declare any actions." +
                    " Please call `useAction()` methods."
        }
        return actions {
            action.putAll(collected)
        }
    }
}
