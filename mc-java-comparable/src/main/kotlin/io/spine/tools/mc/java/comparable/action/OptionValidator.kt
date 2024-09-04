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

import io.spine.option.CompareByOption
import io.spine.protodata.Field
import io.spine.protodata.MessageType
import io.spine.protodata.PrimitiveType
import io.spine.protodata.PrimitiveType.PT_UNKNOWN
import io.spine.protodata.PrimitiveType.TYPE_BYTES
import io.spine.protodata.Type
import io.spine.protodata.TypeName
import io.spine.protodata.isMessage
import io.spine.protodata.isPrimitive

/**
 * Validates fields that are going to participate in a comparison against their types.
 *
 * Not all types are comparable by default. A field type should be one of the following:
 *
 * 1. Have any [PrimitiveType], except for PT_UNKNOWN and TYPE_BYTES.
 * 2. Be an enum.
 * 3. Be a comparable message. Such a message is also marked with `compare_by` annotation.
 * 4. Be a well-known type like Timestamp, Duration or Value messages, for which comparators are
 * provided by default.
 */
// TODO:2024-09-04:yevhenii.nadtochii: Move to model? It is used by a specific action.
//  Though, `ImplementComparable` should also not work if this validation fails.
internal class OptionValidator(private val findMessage: (TypeName) -> MessageType) {

    fun check(option: CompareByOption, message: MessageType) {
        val passedFields = option.fieldList
        check(passedFields.isNotEmpty()) {
            "One or more fields should be specified for `compare_by` option."
        }
        passedFields
            .map { toTyped(it, message) }
            .forEach(::check)
    }

    private fun toTyped(fieldName: String, containedMessage: MessageType): Field =
        if (fieldName.contains(".")) {
            searchRecursively(fieldName, containedMessage)
        } else {
            containedMessage.getField(fieldName)
        }

    private fun searchRecursively(fieldPath: String, currentMessage: MessageType): Field {
        if (!fieldPath.contains(".")) {
            return currentMessage.getField(fieldPath)
        }

        val currentFieldName = fieldPath.substringBefore(".")
        val currentField = currentMessage.getField(currentFieldName)
        checkIntermediate(currentField)

        val restFields = fieldPath.substringAfter(".")
        val nextMessage = findMessage(currentField.type.message) // We are sure it is a message.
        return searchRecursively(restFields, nextMessage)
    }

    private fun checkIntermediate(field: Field) {
        check(field.hasSingle()) // Lists, maps and one-ofs are prohibited.
        check(field.isMessage) // Only a message can be an intermediate part of the "field path".
    }

    private fun check(field: Field) {
        check(field.hasSingle()) // Lists, maps and one-ofs are prohibited.
        val type = field.type
        when {
            type.isPrimitive -> check(type.primitive.isComparable) {
                "Unsupported primitive type: `${type.primitive}`"
            }

            type.isMessage -> check(type.isComparable) {
                "The passed field has a non-comparable type: `${type.message}`."
            }

            // Enums are passed with no checks.
        }
    }

    private val Type.isComparable: Boolean
        get() {
            val message = findMessage(this.message) // We are sure it is a message.
            return message.optionList.any { it.name == "compare_by" }
        }
}

private fun MessageType.getField(name: String): Field =
    fieldList.find { it.name.value == name }
        ?: error("Field `$name` not found in `$this`.")

private val PrimitiveType.isComparable
    get() = this != PT_UNKNOWN && this != TYPE_BYTES
