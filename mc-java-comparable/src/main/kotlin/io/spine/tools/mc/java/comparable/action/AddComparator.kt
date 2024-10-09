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
import io.spine.compare.ComparatorRegistry
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.PrimitiveType.PT_UNKNOWN
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.isEnum
import io.spine.protodata.ast.isMessage
import io.spine.protodata.ast.isPrimitive
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.java.render.DirectMessageAction
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.comparable.ComparableMessage
import io.spine.tools.mc.java.comparable.action.ProtoValueMessages.isProtoValueMessage
import io.spine.tools.mc.java.comparable.hasCompareByOption
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory

/**
 * Builds and inserts a static `comparator` field into the messages that qualify as comparable.
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

    // TODO:2024-09-01:yevhenii.nadtochii: `TypeRenderer` has this view when creates the action.
    private val option = select(ComparableMessage::class.java)
        .findById(type)!!
        .option

    override fun doRender() {
        val optionFields = option.fieldList
        require(optionFields.isNotEmpty()) {
            "`compare_by` option should have at least one field specified."
        }

        val classLookup = ClassLookup(context!!)
        val messageLookup = MessageLookup(context!!)
        val fieldsLookup = FieldLookup(messageLookup)
        val comparator = ComparatorBuilder(cls)

        optionFields
            .associateWith { fieldPath -> fieldsLookup.resolve(fieldPath, type) }
            .forEach { (fieldPath, field) ->
                comparator.comparingBy(fieldPath, field, messageLookup, classLookup)
            }

        if (option.descending) {
            comparator.reversed()
        }

        val messageField = elementFactory.createFieldFromText(comparator.build(), cls)
            .apply { addFirst(GeneratedAnnotation.create()) }

        // `cls.addFirst()` puts it right BEFORE the class definition, but we need it inside.
        cls.addAfter(messageField, cls.lBrace)
    }

    /**
     * Checks if the given [field] can participate in the comparison.
     *
     * The requirements to the passed fields are described in docs to `compare_by`
     * option in detail. This method enforces those requirements.
     *
     * In short, the following types are allowed:
     *
     * 1. All primitives except for byte array.
     * 2. Enumerations (Java enums are implicitly comparable).
     * 3. Messages with `compare_by` option.
     * 4. [ProtoValueMessages] messages.
     * 5. External messages (which are not subject of the ongoing codegen session),
     * for which [ComparatorRegistry] has a comparator.
     */
    private fun ComparatorBuilder.comparingBy(
        path: FieldPath,
        field: Field,
        messageLookup: MessageLookup,
        classLookup: ClassLookup
    ) {
        check(field.hasSingle()) {
            "The repeated fields can't participate in comparison: `${field.name}`."
        }

        val fieldType = field.type
        when {
            fieldType.isPrimitive -> {
                check(fieldType.primitive.isComparable) {
                    "The field has a non-comparable type: `${fieldType.primitive}`."
                }
                comparingBy(path)
            }

            fieldType.isMessage -> {
                val message = messageLookup.query(fieldType.message)
                val hasCompareByOption = message.hasCompareByOption
                val clazz = classLookup.query(message)

                // If the field type is not external, it should have the option.
                // We don't handle providing comparators for messages that are being generated now.
                if (clazz == null) {
                    check(hasCompareByOption) {
                        "The field has a non-comparable type: `${fieldType.message}`."
                    }
                    comparingBy(path)
                    return
                }

                if (clazz.isProtoValueMessage) {
                    comparingBy("$path.value")
                    return
                }

                val hasRegistryComparator = ComparatorRegistry.contains(clazz)
                check(hasRegistryComparator xor hasCompareByOption) {
                    "The field type should either be comparable or have a comparator provided " +
                            "in the `ComparatorRegistry`: `${fieldType.message}`."
                }

                if (hasCompareByOption) {
                    comparingBy(path)
                } else {
                    val className = "${clazz.canonicalName}.class"
                    val comparator = "io.spine.compare.ComparatorRegistry.get($className)"
                    comparingBy(path, comparator)
                }
            }

            else -> {
                check(fieldType.isEnum) {
                    "The field has an unrecognized type: `$fieldType`."
                }
                comparingBy(path)
            }
        }
    }
}

private val PrimitiveType.isComparable
    get() = this != PT_UNKNOWN && this != TYPE_BYTES
