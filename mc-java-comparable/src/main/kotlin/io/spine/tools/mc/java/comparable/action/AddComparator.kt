/*
 * Copyright 2025, TeamDev. All rights reserved.
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
import com.google.protobuf.GeneratedMessageV3
import io.spine.base.FieldPath
import io.spine.base.copy
import io.spine.base.fieldPath
import io.spine.compare.ComparatorRegistry
import io.spine.option.CompareByOption
import io.spine.protobuf.defaultInstance
import io.spine.protodata.Compilation
import io.spine.protodata.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.Option
import io.spine.protodata.ast.PrimitiveType.PT_UNKNOWN
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.cardinality
import io.spine.protodata.ast.find
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.unpack
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.MethodCall
import io.spine.protodata.java.javaClass
import io.spine.protodata.java.render.DirectMessageAction
import io.spine.protodata.java.toPsi
import io.spine.protodata.render.SourceFile
import io.spine.protodata.type.fileOf
import io.spine.string.simply
import io.spine.tools.code.Java
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.base.joined
import io.spine.tools.mc.java.base.resolve
import io.spine.tools.mc.java.comparable.WellKnownComparables.isWellKnownComparable
import io.spine.tools.psi.addFirst
import io.spine.type.typeName
import java.io.File

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

    /** The declaration of the [CompareByOption] option in the [type]. */
    private val option: Option by lazy {
        type.option<CompareByOption>()
    }

    /** The full path to the proto file declaring the [type]. */
    private val protoSource: File by lazy {
        typeSystem.fileOf(type)!!
    }

    override fun doRender() {
        val compareBy = option.unpack<CompareByOption>()
        val comparisonFields = compareBy.fieldList.map(::toComparisonField)
        require(comparisonFields.isNotEmpty()) {
            "The `(compare_by)` option should have at least one field specified."
        }

        val comparator = ComparatorBuilder(cls, compareBy.descending)
        comparisonFields.forEach { comparator.comparingBy(it) }

        val javaField = comparator.build().toPsi()
            .apply { addFirst(GeneratedAnnotation.forPsi()) }
        cls.addAfter(javaField, cls.lBrace)
    }

    /**
     * Maps the field [path] to an appropriate instance of [ComparisonField],
     * depending on the field type.
     */
    private fun toComparisonField(path: String): ComparisonField {
        val fieldPath = path.toFieldPath()
        val field = try {
            typeSystem.resolve(fieldPath, type)
        } catch (e: IllegalStateException) {
            Compilation.error(
                protoSource, option.span.startLine, option.span.startColumn,
                "Unable to find a field with the path `$path` in the type `${type.qualifiedName}`."
            )
        }

        val fieldType = field.type

        check(field.type.cardinality == CARDINALITY_SINGLE) {
            "Repeated fields or maps can't participate in comparison. " +
                    "The invalid field: `$field`, its type: `$fieldType`. " +
                    "Please, make sure the type of the passed field is compatible with " +
                    "the `(compare_by)` option."
        }

        return when {
            fieldType.isPrimitive -> PrimitiveComparisonField(fieldPath, fieldType.primitive)
            fieldType.isEnum -> EnumComparisonField(fieldPath)
            fieldType.isMessage -> {
                val typeName = fieldType.message
                val (type, header) = typeSystem.findMessage(typeName)!!
                val javaClass = type.javaClass(header)
                if (javaClass == null) {
                    MessageComparisonField(fieldPath, type)
                } else {
                    ExternalMessageComparisonField(fieldPath, type, javaClass)
                }
            }

            else -> unsupportedFieldType(fieldPath, fieldType)
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
     * 2. All enumerations (Java enums are implicitly comparable).
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
                    "The type of the `${path.joined}` field should have the `(compare_by)` " +
                            "option itself to participate in comparison."
                }
                comparingBy(path)
            }

            is ExternalMessageComparisonField -> comparingBy(field)
        }
    }

    /**
     * Adds the external message field to this [ComparatorBuilder].
     *
     * This method expects the given [field] to be one of the following:
     *
     * 1. An external message with [CompareByOption] and without
     * a comparator in [ComparatorRegistry].
     * 2. An external message without the option, but with a comparator
     * in the registry.
     * 3. [Well-known comparable][io.spine.tools.mc.java.comparable.WellKnownComparables].
     */
    private fun ComparatorBuilder.comparingBy(field: ExternalMessageComparisonField) {
        val path = field.path
        val clazz = field.clazz
        val fromRegistry = ComparatorRegistry.find(clazz)
        val hasCompareByOption = field.type.hasCompareByOption
        when {
            hasCompareByOption -> {
                check(fromRegistry == null) {
                    "The type of the `${path.joined}` field must either have the `(compare_by)` " +
                            "option specified OR have a `Comparator` registered in " +
                            "the `ComparatorRegistry`, but not both simultaneously."
                }
                comparingBy(path)
            }

            fromRegistry != null -> {
                val comparator = MethodCall<Comparator<Any>>(
                    ClassName(ComparatorRegistry::class),
                    "get",
                    ClassName(clazz).clazz
                )
                comparingBy(path, comparator)
            }

            clazz.isWellKnownComparable -> comparingBy(path.copy { fieldName.add("value") })

            else -> unsupportedFieldType(path, field.type)
        }
    }

    /**
     * Throws [IllegalStateException] to indicate that the passed [fieldPath]
     * denotes a field with an unsupported type.
     *
     * This error is meant to serve as a safe net for cases when the passed field
     * type is unexpected for the plugin. For example, Protobuf may introduce a new field
     * type or cardinality. If this happens, we should add the support of such a type
     * to this plugin. Otherwise, "safe net" errors are thrown.
     *
     * Note: the names of method arguments are prefixed with "field" intentionally.
     * So not to clash with [type] class member.
     */
    private fun unsupportedFieldType(fieldPath: FieldPath, fieldType: Any?): Nothing = error(
        "The field `$fieldPath` declared in the message `$type` is not of the supported type " +
                " (`$fieldType`) for the comparison. Supported field types are: " +
                "primitives, enums, and comparable messages. Checks out docs to " +
                "the `(compare_by)` option for details."
    )
}

private val MessageType.hasCompareByOption: Boolean
    get() = optionList.find<CompareByOption>() != null

/**
 * Transforms this potentially dot-delimited string into [FieldPath].
 *
 * If there are no dots in this string the returned [FieldPath] contains
 * only this string.
 */
private fun String.toFieldPath() = fieldPath {
    fieldName.addAll(this@toFieldPath.split("."))
}

/**
 * Finds the instance of [Option] which contains the message with the given
 * type [T] as its value.
 *
 * @throws IllegalStateException if the option with the given type is not found.
 */
private inline fun <reified T : GeneratedMessageV3> MessageType.option(): Option {
    val optionUrl = T::class.java.defaultInstance.typeName.toUrl().value()
    optionList.find { opt -> opt.value.typeUrl == optionUrl }?.let { return it }
        ?: error(
            "The message `${name.qualifiedName}` must have the `${simply<T>()}` option."
        )
}
