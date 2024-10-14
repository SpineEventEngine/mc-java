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
import io.spine.base.copy
import io.spine.base.fieldPath
import io.spine.compare.ComparatorRegistry
import io.spine.option.CompareByOption
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.PrimitiveType.PT_UNKNOWN
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.isEnum
import io.spine.protodata.ast.isMessage
import io.spine.protodata.ast.isPrimitive
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.java.javaClassName
import io.spine.protodata.java.render.DirectMessageAction
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.base.FieldLookup
import io.spine.tools.mc.java.base.joined
import io.spine.tools.mc.java.comparable.ComparableMessage
import io.spine.tools.mc.java.comparable.WellKnownComparables.isWellKnownComparable
import io.spine.tools.mc.java.comparable.hasCompareByOption
import io.spine.tools.mc.java.findJavaClass
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory

/**
 * Builds and inserts a static `comparator` field into the messages that qualify
 * as comparable.
 *
 * @param type The type of the message.
 * @param file The source code to which the action is applied.
 * @param context The code generation context in which this action runs.
 */
public class AddComparator(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : DirectMessageAction<Empty>(type, file, Empty.getDefaultInstance(), context) {

    private val fieldsLookup = FieldLookup(typeSystem!!)

    override fun doRender() {
        val option = compareByOption(type)
        val comparisonFields = option.fieldList.map(::toComparisonField)
        require(comparisonFields.isNotEmpty()) {
            "The `compare_by` option should have at least one field specified."
        }

        val comparator = ComparatorBuilder(cls, option.descending)
        comparisonFields.forEach { comparator.comparingBy(it) }

        val javaField = elementFactory.createFieldFromText(comparator.build(), cls)
            .apply { addFirst(GeneratedAnnotation.create()) }
        cls.addAfter(javaField, cls.lBrace)
    }

    /**
     * Queries the [CompareByOption] option for the given message [type].
     */
    private fun compareByOption(type: MessageType) = select(ComparableMessage::class.java)
        .findById(type)!!
        .option

    /**
     * Maps the field [path] to an appropriate instance of [ComparisonField].
     */
    private fun toComparisonField(path: String): ComparisonField {
        val fieldPath = fieldPath { fieldName.addAll(path.split(".")) }
        val field = fieldsLookup.resolve(fieldPath, rootMessage = type)
        val fieldType = field.type

        check(field.hasSingle()) {
            "The repeated fields and maps can't participate in comparison. " +
                    "The invalid field: `$field`, its type: `$fieldType`. " +
                    "Please, make sure the type of the passed field is compatible with " +
                    "`compare_by` option."
        }

        return when {
            fieldType.isPrimitive -> PrimitiveComparisonField(fieldPath, fieldType.primitive)
            fieldType.isEnum -> EnumComparisonField(fieldPath)
            fieldType.isMessage -> {
                val typeName = fieldType.message
                val (type, header) = typeSystem!!.findMessage(typeName)!!
                val javaClassName = type.javaClassName(header)
                val javaClass = javaClassName.findJavaClass()
                if (javaClass == null) {
                    MessageComparisonField(fieldPath, type)
                } else {
                    ExternalMessageComparisonField(fieldPath, type, javaClass)
                }
            }

            else -> error(
                "The field `$path` has an unrecognized Proto field type: `${this.type}`. " +
                        "The supported Proto fields: primitives, enums and messages."
            )
        }
    }

    /**
     * Adds the comparison [field] to this [ComparatorBuilder].
     *
     * The requirements to the comparison fields are described in docs to [CompareByOption]
     * option in detail.
     *
     * In short, the following fields are accepted:
     *
     * 1. All primitives except for byte arrays.
     * 2. Enumerations (Java enums are implicitly comparable).
     * 3. Messages with [CompareByOption] option.
     * 4. External messages for which [ComparatorRegistry] has a comparator.
     * 5. [WellKnownComparables][io.spine.tools.mc.java.comparable.WellKnownComparables].
     */
    private fun ComparatorBuilder.comparingBy(field: ComparisonField) {
        val path = field.path
        when (field) {
            is EnumComparisonField -> comparingBy(path)

            is PrimitiveComparisonField -> {
                check(field.type != PT_UNKNOWN) {
                    "The field `${path.joined}` has an unknown primitive type: `$type`."
                }
                check(field.type != TYPE_BYTES) {
                    "The field `${path.joined}` has a non-comparable `bytes[]` type."
                }
                comparingBy(path)
            }

            is MessageComparisonField -> {
                check(field.type.hasCompareByOption) {
                    "The type of `${path.joined}` field should have `compare_by` option itself " +
                            "to participate in comparison."
                }
                comparingBy(path)
            }

            is ExternalMessageComparisonField -> comparingBy(field)
        }
    }

    /**
     * Adds the message field to this [ComparatorBuilder].
     *
     * This method enforces the following rules:
     *
     * 1. Only external messages (Java code for which is NOT being generated now)
     * are eligible for having a comparator in [ComparatorRegistry].
     * 2. If the message has a [CompareByOption], then the registry should NOT
     * have a comparator for this type. Otherwise, it is unclear what to use.
     * 3. [WellKnownComparables][io.spine.tools.mc.java.comparable.WellKnownComparables]
     * are allowed to participate in comparison by default.
     */
    private fun ComparatorBuilder.comparingBy(field: ExternalMessageComparisonField) {
        val path = field.path
        val clazz = field.clazz
        val fromRegistry = ComparatorRegistry.find(clazz)
        val hasCompareByOption = field.type.hasCompareByOption
        when {
            hasCompareByOption -> {
                check(fromRegistry == null) {
                    "The type of `${path.joined}` field must either have `compare_by` options OR " +
                            "have a `Comparator` registered in the `ComparatorRegistry`, " +
                            "but not both simultaneously."
                }
                comparingBy(path)
            }

            fromRegistry != null -> {
                val className = "${clazz.canonicalName}.class"
                val comparator = "io.spine.compare.ComparatorRegistry.get($className)"
                comparingBy(path, comparator)
            }

            clazz.isWellKnownComparable -> comparingBy(path.copy { fieldName.add("value") })

            else -> error(
                "The field `$path` has an unrecognized message type: `$type`. " +
                        "Check out the supported types in docs to `compare_by` option."
            )
        }
    }
}
