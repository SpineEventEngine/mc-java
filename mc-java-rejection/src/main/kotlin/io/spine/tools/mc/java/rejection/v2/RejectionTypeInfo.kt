/*
 * Copyright 2023, TeamDev. All rights reserved.
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
package io.spine.tools.mc.java.rejection.v2

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableList.toImmutableList
import io.spine.base.RejectionType
import io.spine.protodata.Field
import io.spine.protodata.MessageType
import io.spine.protodata.ProtobufSourceFile
import io.spine.tools.mc.java.field.toField

/**
 * An adapter between [RejectionType] and [MessageType].
 */
internal class RejectionTypeInfo {

    private val javaPackage: String
    private val simpleJavaClassName: String
    private val leadingComments: String
    private val fields: ImmutableList<Field>

    constructor(type: RejectionType) {
        javaPackage = type.javaPackage().toString()
        simpleJavaClassName = type.simpleJavaClassName().toString()
        leadingComments = type.leadingComments().orElse("")
        fields = type.fields()
            .stream()
            .map { fd -> fd.toField() }
            .collect(toImmutableList())
    }

    constructor(file: ProtobufSourceFile, type: MessageType) {
        javaPackage = file.javaPackage()
        simpleJavaClassName = type.name.simpleName
        leadingComments = type.doc.leadingComment
        fields = ImmutableList.copyOf(type.fieldList)
    }

    fun javaPackage(): String {
        return javaPackage
    }

    fun simpleJavaClassName(): String {
        return simpleJavaClassName
    }

    fun leadingComments(): String {
        return leadingComments
    }

    fun fields(): ImmutableList<Field> {
        return fields
    }
}

