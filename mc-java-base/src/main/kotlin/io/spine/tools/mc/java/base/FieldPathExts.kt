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

package io.spine.tools.mc.java.base

import io.spine.base.FieldPath
import io.spine.base.fieldPath
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.field
import io.spine.protodata.type.TypeSystem

/**
 * Tells if this [FieldPath] doesn't denote a nested field.
 */
public val FieldPath.isNotNested: Boolean
    get() = fieldNameList.size == 1

/**
 * Returns this [FieldPath] as a single [String], where the field names
 * are separated with a dot.
 *
 * For example, `citizen.passport.firstName`.
 */
public val FieldPath.joined: String
    get() = fieldNameList.joinToString(".")

/**
 * Returns the root field's name of this [FieldPath].
 *
 * @throws [NoSuchElementException] if the path is empty.
 */
public val FieldPath.root: String
    get() = fieldNameList.first()

/**
 * Resolves this [FieldPath] against the given [message].
 *
 * @param message The message that contains the root field of this [FieldPath].
 * @param typeSystem The known Protobuf types used for metadata querying.
 */
public fun FieldPath.resolve(message: MessageType, typeSystem: TypeSystem): Field {
    if (this.isNotNested) {
        return message.field(root)
    }
    val currentField = message.field(root)
    val remainingFields = fieldNameList.drop(1)
    val remainingPath = fieldPath { fieldName.addAll(remainingFields) }
    val nextMessage = typeSystem.findMessage(currentField.type.message)!!.first
    return remainingPath.resolve(nextMessage, typeSystem)
}
