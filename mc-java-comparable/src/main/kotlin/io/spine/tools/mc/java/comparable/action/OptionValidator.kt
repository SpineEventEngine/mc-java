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
import io.spine.protodata.TypeName
import io.spine.protodata.isEnum
import io.spine.protodata.isMessage
import io.spine.protodata.isPrimitive
import io.spine.protodata.qualifiedName

/**
 * Validates fields specified in `compare_by` option, enforcing the option's contract.
 *
 * The allowed field types are the following:
 *
 * 1. All primitive types (except for `bytes`) and enums.
 * 2. Comparable messages (which are also marked with `compare_by` option).
 * 3. Some Protobuf well-known types: Timestamp, Duration and *Value messages.
 *
 * See also: [Protobuf Docs | Well-known types](https://protobuf.dev/reference/protobuf/google.protobuf/).
 */
// TODO:2024-09-04:yevhenii.nadtochii: Move to the model? It is used by a specific action.
//  Though, `ImplementComparable` should also not work if this validation fails.
//  Naturally, this code belongs to `Proto sources analysis` stage, which is done in the context.
internal class OptionValidator(private val findMessage: (TypeName) -> MessageType) {

    // TODO:2024-09-04:yevhenii.nadtochii: Support well-known types.
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

            type.isMessage -> {
                val message = findMessage(type.message)
                check(message.isComparable || message.isAllowedWellKnown) {
                    "The passed field has a non-comparable type: `${type.message}`."
                }
            }

            else -> {
                // Enums are passed with no checks.
                check(type.isEnum) {
                    "Unrecognized Proto type: `$type`."
                }
            }
        }
    }

    private val MessageType.isAllowedWellKnown: Boolean
        get() = allowedWellKnown.contains(qualifiedName)

    private val MessageType.isComparable: Boolean
        get() = optionList.any { it.name == "compare_by" }
}

private fun MessageType.getField(name: String): Field =
    fieldList.find { it.name.value == name }
        ?: error("Field `$name` not found in `$this`.")

private val PrimitiveType.isComparable
    get() = this != PT_UNKNOWN && this != TYPE_BYTES

private val allowedWellKnown = listOf(
    "google.protobuf.Timestamp",
    "google.protobuf.Duration",
    "google.protobuf.BoolValue",
    "google.protobuf.DoubleValue",
    "google.protobuf.FloatValue",
    "google.protobuf.Int32Value",
    "google.protobuf.Int64Value",
    "google.protobuf.UInt32Value",
    "google.protobuf.UInt64Value",
    "google.protobuf.StringValue",
)
