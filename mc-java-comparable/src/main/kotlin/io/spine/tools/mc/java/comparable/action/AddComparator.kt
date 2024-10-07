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
import io.spine.tools.mc.java.comparable.isComparable
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory

/**
 * Inserts a `comparator` field into the messages that qualify as comparable.
 *
 * This action also validates that the passed fields are eligible to participate
 * in the comparison. See [validate] method for details.
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
            "`compare_by` option should have at least one field specified in `$messageClass`."
        }

        val context = context!!
        val messageLookup = MessageLookup(context)
        val fieldsLookup = FieldLookup(messageLookup)
        val classLookup = ClassLookup(context)
        val comparator = ComparatorBuilder(cls)

        val rootMessage = type
        optionFields
            .associateWith { fieldPath -> fieldsLookup.resolve(fieldPath, rootMessage) }
            .forEach { (fieldPath, field) ->
                validate(field, messageLookup,classLookup)
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
     * 5. Messages, for which [ComparatorRegistry] has a comparator.
     */
    private fun validate(field: Field, messageLookup: MessageLookup, classLookup: ClassLookup) {
        check(field.hasSingle()) {
            "`${field.name}` is not a single-value field and can not participate in comparison."
        }
        val type = field.type
        when {
            type.isPrimitive -> check(type.primitive.isComparable) {
                "The passed field has a non-comparable primitive type: `${type.primitive}`"
            }

            type.isMessage -> {
                val message = messageLookup.query(type.message)
                if (!message.isComparable) {
                    val clazz = classLookup.query(message)
                    check(clazz.isProtoValueMessage || ComparatorRegistry.contains(clazz)) {
                        "The field has a non-comparable message type: `${type.message}`, ${clazz}."
                    }
                }
            }

            else -> check(type.isEnum) {
                "The passed field has an unrecognized type: `$type`."
            }
        }
    }

    private fun ComparatorBuilder.comparingBy(
        path: FieldPath,
        field: Field,
        messageLookup: MessageLookup,
        classLookup: ClassLookup
    ) {
        val type = field.type
        if (type.isMessage) {
            val message = messageLookup.query(type.message)
            if (message.isComparable) {
                comparingBy(path)
            } else {
                val clazz = classLookup.query(message)
                if (clazz.isProtoValueMessage) {
                    comparingBy("$path.value")
                } else if (ComparatorRegistry.contains(clazz)) {
                    val canonical = clazz.canonicalName
                    val comparator = "io.spine.compare.ComparatorRegistry.INSTANCE.get($canonical.class)"
                    comparingBy(path, comparator)
                }
            }
        } else {
            comparingBy(path)
        }
    }
}

private val PrimitiveType.isComparable
    get() = this != PT_UNKNOWN && this != TYPE_BYTES
