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

package io.spine.tools.mc.java.entity.query

import io.spine.protodata.CodegenContext
import io.spine.protodata.Field
import io.spine.protodata.MessageType
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.javaClassName
import io.spine.protodata.java.typeReference
import io.spine.tools.mc.java.NestedClassAction
import io.spine.tools.mc.java.entity.idField
import io.spine.tools.mc.java.settings.Entities

/**
 * Abstract base for classes generating code for supporting queries for
 * entity state types.
 */
internal abstract class QuerySupportClass(
    className: String,
    type: MessageType,
    protected val settings: Entities,
    context: CodegenContext
) : NestedClassAction(type, className, context) {

    /**
     * The class of the entity state.
     */
    protected val entityStateClass: ClassName by lazy {
        type.javaClassName(typeSystem!!)
    }

    /**
     * The simple name of the entity state type which is equivalent of
     * the simple class name for the generated Java class.
     */
    protected val stateType: String = type.name.simpleName

    /**
     * The identifier field of the entity state.
     */
    val idField: Field by lazy {
        type.idField(settings)
    }

    /**
     * The type of the [first][idField] entity state type field.
     */
    val idType: String by lazy {
        idField.typeReference(entityStateClass, typeSystem!!)
    }
}
