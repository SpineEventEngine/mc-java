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

import com.google.protobuf.Empty
import io.spine.protodata.CodegenContext
import io.spine.protodata.Field
import io.spine.protodata.MessageType
import io.spine.protodata.PrimitiveType
import io.spine.protodata.PrimitiveType.PT_UNKNOWN
import io.spine.protodata.PrimitiveType.TYPE_BYTES
import io.spine.protodata.isEnum
import io.spine.protodata.isMessage
import io.spine.protodata.isPrimitive
import io.spine.protodata.qualifiedName
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.typeName
import io.spine.tools.code.Java
import io.spine.tools.mc.java.DirectMessageAction
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.comparable.ComparableMessage
import io.spine.tools.mc.java.comparable.isComparable
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory

/**
 * Inserts a `comparator` field into the messages that qualifies as comparable.
 *
 * This action also validates that the passed fields are eligible to participate
 * in the comparison. See [validate] method for details.
 *
 * @type type The type of the message.
 * @type file The source code to which the action is applied.
 * @type context The code generation context in which this action runs.
 */
public class AddComparator(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : DirectMessageAction<Empty>(type, file, Empty.getDefaultInstance(), context) {

    private val messages = MessageLookup(context)
    private val fields = FieldLookup(messages)
    private val comparator = ComparatorBuilder(cls)

    // TODO:2024-09-01:yevhenii.nadtochii: `TypeRenderer` has this view when creates the action.
    private val option = select(ComparableMessage::class.java)
        .findById(type)!!
        .option

    override fun doRender() {
        val optionFields = option.fieldList
        require(optionFields.isNotEmpty()) {
            "`compare_by` option should have at least one field specified in `$messageClass`."
        }

        val targetMessage = type
        optionFields
            .associateWith { fields.find(it, targetMessage) }
            .forEach(::comparingBy)

        val messageField = elementFactory.createFieldFromText(comparator.build(), cls)
            .apply { addFirst(GeneratedAnnotation.create()) }
        cls.addAfter(messageField, cls.lBrace)
    }

    private fun comparingBy(path: FieldPath, field: Field) {
        validate(field)
        if (field.type.isMessage) {
            when (field.type.typeName.qualifiedName) {
                wellKnownTimestamp -> {
                    comparator.comparingBy(path, "com.google.protobuf.util.Timestamps.comparator()")
                }

                wellKnownDuration -> {
                    comparator.comparingBy(path, "com.google.protobuf.util.Durations.comparator()")
                }

                in wellKnownValues -> {
                    comparator.comparingBy("$path.value")
                }

                else -> comparator.comparingBy(path)
            }
        } else {
            comparator.comparingBy(path)
        }
    }

    private fun validate(field: Field) {
        check(field.hasSingle()) {
            "`${field.name}` is not a single-value field and can not participate in comparison."
        }
        val type = field.type
        when {
            type.isPrimitive -> check(type.primitive.isComparable) {
                "Unsupported primitive type: `${type.primitive}`"
            }

            type.isMessage -> {
                val message = messages.find(type.message)
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
}

private val MessageType.isAllowedWellKnown: Boolean
    get() = allowedWellKnown.contains(qualifiedName)

private val PrimitiveType.isComparable
    get() = this != PT_UNKNOWN && this != TYPE_BYTES

private const val wellKnownDuration = "google.protobuf.Duration"
private const val wellKnownTimestamp = "google.protobuf.Timestamp"
private val wellKnownValues = listOf(
    "google.protobuf.BoolValue",
    "google.protobuf.DoubleValue",
    "google.protobuf.FloatValue",
    "google.protobuf.Int32Value",
    "google.protobuf.Int64Value",
    "google.protobuf.UInt32Value",
    "google.protobuf.UInt64Value",
    "google.protobuf.StringValue",
)

private val allowedWellKnown = wellKnownValues + wellKnownDuration + wellKnownTimestamp

