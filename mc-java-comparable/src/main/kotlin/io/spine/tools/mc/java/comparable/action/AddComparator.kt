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
import io.spine.protodata.MessageType
import io.spine.protodata.PrimitiveType
import io.spine.protodata.PrimitiveType.PT_UNKNOWN
import io.spine.protodata.PrimitiveType.TYPE_BYTES
import io.spine.protodata.TypeName
import io.spine.protodata.isMessage
import io.spine.protodata.isPrimitive
import io.spine.protodata.renderer.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.mc.java.DirectMessageAction
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.comparable.ComparableActions
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory
import org.intellij.lang.annotations.Language

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

    private val clsName = cls.name!!

    // TODO:2024-09-01:yevhenii.nadtochii: Can we ask a `TypeRenderer` pass it to us?
    //  This view contains a discovered `compare_by` option.
    private val option = select(ComparableActions::class.java)
        .findById(type)!!
        .option

    override fun doRender() {
        val field = elementFactory.createFieldFromText(comparator(), cls)
        field.addFirst(GeneratedAnnotation.create())

        // TODO:2024-09-02:yevhenii.nadtochii: addFirst() and addLast() extension are inconsistent.
        cls.addAfter(field, cls.lBrace)
    }

    @Language("JAVA")
    @Suppress("LocalVariableName") // Simplifies reading of string patterns.
    private fun comparator(): String {
        val MESSAGE = type.name.simpleName
        val fields = option.fieldList.iterator()
        val declaration = buildString {
            append("Comparator.comparing($MESSAGE::${next(fields)})")
            while (fields.hasNext()) {
                append(".thenComparing($MESSAGE::${next(fields)})")
            }
        }
        return "private static final Comparator<$clsName> comparator = $declaration;"
    }

    // TODO:2024-09-02:yevhenii.nadtochii: Support nested fields.
    private fun next(fields: Iterator<String>): String {
        val requested = fields.next()
        val declaration = type.fieldList.find { it.name.value == requested }!!
            .also { check(it.hasSingle()) } // Lists, maps and one-ofs are prohibited.
        val type = declaration.type
        when {
            type.isPrimitive && type.primitive.isNotComparable -> error(
                "Unsupported primitive type: `${type.primitive}`"
            )
            type.isMessage && type.message.isNotComparable -> error(
                "The passed field has a non-comparable type: `${type.message}`."
            )
        }
        val fieldName = toJavaFieldName(requested)
        return "get$fieldName"
    }
}

private fun toJavaFieldName(protobufFieldName: String): String {
    val parts = protobufFieldName.split("_")
    val joined = parts.joinToString("") { part ->
        part.replaceFirstChar { it.uppercaseChar() }
    }
    return joined.replaceFirstChar { it.uppercaseChar() }
}

private val PrimitiveType.isNotComparable
    get() = this == TYPE_BYTES || this == PT_UNKNOWN

// TODO:2024-09-02:yevhenii.nadtochii: Check if a message implements `Comparable`.
private val TypeName.isNotComparable
    get() = simpleName.isNotEmpty()
