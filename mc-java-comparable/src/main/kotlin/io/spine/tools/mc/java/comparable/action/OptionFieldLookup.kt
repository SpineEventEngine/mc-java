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

package io.spine.tools.mc.java.comparable.action

import io.spine.protodata.Field
import io.spine.protodata.MessageType
import io.spine.protodata.TypeName
import io.spine.protodata.isMessage

internal class OptionFieldLookup(private val findMessage: (TypeName) -> MessageType) {

    fun find(path: FieldPath, messageType: MessageType): Pair<Field, MessageType> =
        if (path.isNotNested) {
            messageType.getField(path) to messageType
        } else {
            searchRecursively(path, messageType)
        }

    private fun searchRecursively(path: FieldPath, message: MessageType): Pair<Field, MessageType> {
        if (path.isNotNested) {
            return message.getField(path) to message
        }

        val currentFieldName = path.substringBefore(".")
        val currentField = message.getField(currentFieldName)
        checkIntermediate(currentField)

        val remainingFields = path.substringAfter(".")
        val nextMessage = findMessage(currentField.type.message) // We are sure it is a message.
        return searchRecursively(remainingFields, nextMessage)
    }

    // Otherwise, `findMessage()` will for sure throw.
    private fun checkIntermediate(field: Field) {
        check(field.hasSingle()) // Lists, maps and one-ofs are prohibited.
        check(field.isMessage) // Only a message can be an intermediate part of the "FieldPath".
    }
}

/**
 * Looks up a field in this [MessageType] by the given [name].
 */
private fun MessageType.getField(name: String): Field =
    fieldList.find { it.name.value == name }
        ?: error("Field `$name` not found in `$this`.")
