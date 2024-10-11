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
import io.spine.base.FieldPath
import io.spine.base.copy
import io.spine.base.fieldPath
import io.spine.compare.ComparatorRegistry
import io.spine.option.CompareByOption
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.PrimitiveType.PT_UNKNOWN
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.Type
import io.spine.protodata.ast.isEnum
import io.spine.protodata.ast.isMessage
import io.spine.protodata.ast.isPrimitive
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.java.render.DirectMessageAction
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.comparable.ComparableMessage
import io.spine.tools.mc.java.comparable.WellKnownComparables.isWellKnownComparable
import io.spine.tools.mc.java.comparable.hasCompareByOption
import io.spine.tools.mc.java.field.FieldLookup
import io.spine.tools.mc.java.field.joined
import io.spine.tools.mc.java.message.ClassLookup
import io.spine.tools.mc.java.message.MessageLookup
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory

/**
 * Builds and inserts a static `comparator` field into the message that qualify as comparable.
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

    private val classLookup = ClassLookup(context)
    private val messageLookup = MessageLookup(context)
    private val option = compareByOption(type)

    override fun doRender() {
        val comparisonFields = option.fields()
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
     *
     * @see comparingByMessage
     */
    private fun ComparatorBuilder.comparingBy(field: ComparisonField) {
        val type = field.type
        val path = field.path
        when {
            type.isPrimitive -> {
                check(type.primitive.isComparable) {
                    "The field `${path.joined}` has a non-comparable primitive type: `$type`."
                }
                comparingBy(path)
            }

            type.isMessage -> comparingByMessage(path, type)

            else -> {
                check(type.isEnum) {
                    "The field `${path.joined}` has an unrecognized type: `$type`. " +
                            "Check out the supported types in docs to `compare_by` option."
                }
                comparingBy(path)
            }
        }
    }

    /**
     * Adds the message field to this [ComparatorBuilder].
     *
     * This method enforces the following rules:
     *
     * 1. Only external messages (Java code for which is NOT being generated now) are eligible for
     * having a comparator in [ComparatorRegistry].
     * 2. If the message has a [CompareByOption], then the registry should NOT have a comparator
     * for this type. Otherwise, it is unclear what to use.
     * 3. [WellKnownComparables][io.spine.tools.mc.java.comparable.WellKnownComparables]
     * are allowed to participate in comparison by default.
     *
     * @param path The field path.
     * @param type The field type.
     */
    private fun ComparatorBuilder.comparingByMessage(path: FieldPath, type: Type) {
        val message = messageLookup.query(type.message)
        val hasCompareByOption = message.hasCompareByOption
        val clazz = classLookup.query(message)
        val fromRegistry = clazz?.let { ComparatorRegistry.find(clazz) }
        when {
            hasCompareByOption -> {
                check(fromRegistry == null) {
                    "The field `${path.joined}` must either implement `Comparable` OR have a `Comparator` " +
                            "registered in the `ComparatorRegistry`, but not both simultaneously."
                }
                comparingBy(path)
            }

            fromRegistry != null -> {
                val className = "${clazz.canonicalName}.class"
                val comparator = "io.spine.compare.ComparatorRegistry.get($className)"
                comparingBy(path, comparator)
            }

            else -> {
                check( clazz != null && clazz.isWellKnownComparable) {
                    "The field `$path` has an unrecognized message type: `$type`. " +
                            "Check out the supported types in docs to `compare_by` option."
                }
                comparingBy(path.copy { fieldName.add("value") })
            }
        }
    }

    /**
     * Queries the [CompareByOption] option for the given message [type].
     */
    private fun compareByOption(type: MessageType) = select(ComparableMessage::class.java)
        .findById(type)!!
        .option

    /**
     * Extracts a list of [ComparisonField]s specified in this [CompareByOption].
     *
     * For nested fields, a deep search of the field metadata is performed.
     */
    private fun CompareByOption.fields(): List<ComparisonField> {
        val fieldsLookup = FieldLookup(messageLookup)
        return fieldList
            .map { path -> fieldPath { fieldName.addAll(path.split(".")) } }
            .associateWith { path -> fieldsLookup.resolve(path, type) }
            .map { (path, field) -> ComparisonField(field, path) }
    }
}

/**
 * Tells if this [PrimitiveType] is comparable.
 *
 * All Protobuf primitives are implicitly comparable in Java except for byte arrays.
 */
private val PrimitiveType.isComparable
    get() = this != PT_UNKNOWN && this != TYPE_BYTES
