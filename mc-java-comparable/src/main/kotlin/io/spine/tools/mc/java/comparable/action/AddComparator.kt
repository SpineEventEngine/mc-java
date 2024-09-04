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
import io.spine.protodata.ProtobufDependency
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.TypeName
import io.spine.protodata.isEnum
import io.spine.protodata.isMessage
import io.spine.protodata.isPrimitive
import io.spine.protodata.qualifiedName
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.typeName
import io.spine.tools.code.Java
import io.spine.tools.mc.java.DirectMessageAction
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.comparable.ComparableActions
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory

/**
 * Updates the code of the message which qualifies as [Comparable] to
 * contain `compareTo()` method.
 *
 * The class is public because its fully qualified name is used as a default
 * value in [ComparableSettings][io.spine.tools.mc.java.gradle.settings.ComparableSettings].
 *
 * @property type the type of the message.
 * @property file the source code to which the action is applied.
 * @property context the code generation context in which this action runs.
 */
public class AddComparator(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : DirectMessageAction<Empty>(type, file, Empty.getDefaultInstance(), context) {

    private val fields = OptionFieldLookup(::findMessage)
    private val comparator = ComparatorBuilder(cls.name!!)

    // TODO:2024-09-01:yevhenii.nadtochii: Can we ask a `TypeRenderer` pass it to us?
    //  This view contains a discovered `compare_by` option.
    private val option = select(ComparableActions::class.java)
        .findById(type)!!
        .option

    // TODO:2024-09-02:yevhenii.nadtochii: PsiClass.addFirst() and addLast() extensions
    //  are inconsistent.
    override fun doRender() {
        val requestedFields = option.fieldList
        require(requestedFields.isNotEmpty()) {
            "`compare_by` option should have at least one field: `$messageClass`."
        }

        requestedFields.associateWith { fields.find(it, type) }
            .forEach { (path, metadata) ->
                val (field, message) = metadata
                check(field)
                append(path, field)
            }


        val field = elementFactory.createFieldFromText(comparator.build(), cls)
        field.addFirst(GeneratedAnnotation.create())
        cls.addAfter(field, cls.lBrace)
    }

    private fun append(path: FieldPath, field: Field) {
        if (field.type.isMessage) {
            when (field.type.typeName.qualifiedName) {
                wellKnownTimestamp -> {
                    val timestamps = elementFactory.createClass("Timestamps")
                    psiFile.importList!!.add(timestamps)
                    comparator.comparingBy(path, "Timestamps.comparator()")
                }

                wellKnownDuration -> {
                    val durations = elementFactory.createClass("Durations")
                    psiFile.importList!!.add(durations)
                    comparator.comparingBy(path, "Durations.comparator()")
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

    private fun findMessage(typeName: TypeName): MessageType {
        val typeUrl = typeName.typeUrl
        val fromFiles = select(ProtobufSourceFile::class.java).all()
            .firstOrNull { it.containsType(typeUrl) }
            ?.typeMap?.get(typeUrl)

        if (fromFiles != null) {
            return fromFiles
        }

        val fromDependencies = select(ProtobufDependency::class.java).all()
            .firstOrNull { it.source.containsType(typeUrl) }
            ?.source?.typeMap?.get(typeUrl)
        return fromDependencies
            ?: error("`$typeUrl` not found in the passed Proto files and its dependencies.")
    }

    private fun check(field: Field) {
        check(field.hasSingle()) {
            "`${field.name}` is not a single-value field."
        }
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
}

private val MessageType.isAllowedWellKnown: Boolean
    get() = allowedWellKnown.contains(qualifiedName)

private val MessageType.isComparable: Boolean
    get() = optionList.any { it.name == "compare_by" }

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

