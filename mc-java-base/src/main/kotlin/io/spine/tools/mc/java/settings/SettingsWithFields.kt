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
package io.spine.tools.mc.java.settings

import com.google.common.collect.ImmutableList
import com.google.protobuf.Message
import io.spine.tools.java.code.Names
import io.spine.tools.mc.java.field.AddFieldClass
import org.checkerframework.checker.signature.qual.FqBinaryName
import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * Code generation settings that include generation of
 * [field classes][io.spine.base.SubscribableField].
 *
 * Model Compiler generates the type-safe API for filtering messages by fields in queries
 * and subscriptions.
 *
 * @param P The Protobuf type reflecting a snapshot of these settings.
 */
public abstract class SettingsWithFields<P : Message> @JvmOverloads internal constructor(
    p: Project,
    defaultActions: Iterable<String> = ImmutableList.of<@FqBinaryName String>()
) : SettingsWithActions<P>(p, defaultActions) {

    private val markFieldsAs: Property<String> = p.objects.property(String::class.java)

    /**
     * Equips the field type with a superclass.
     *
     * @param className The canonical class name of an existing Java class.
     */
    public fun markFieldsAs(className: String) {
        useAction(AddFieldClass::class.java.name, className)
        markFieldsAs.set(className)
    }

    /**
     * Obtains the [GenerateFields] instance containing specified names of
     * field superclasses.
     */
    @Deprecated("Please use `actions()` instead.")
    public fun generateFields(): GenerateFields {
        val generateFields: GenerateFields
        val superclassName = markFieldsAs.getOrElse("")
        generateFields = if (superclassName.isEmpty()) {
            GenerateFields.getDefaultInstance()
        } else {
            GenerateFields.newBuilder()
                .setSuperclass(Names.className(superclassName))
                .build()
        }
        return generateFields
    }
}
