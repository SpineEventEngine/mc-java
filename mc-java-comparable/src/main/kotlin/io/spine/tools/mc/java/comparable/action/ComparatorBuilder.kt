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

import com.google.protobuf.Message
import com.intellij.psi.PsiClass
import io.spine.base.FieldPath
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.InitField
import io.spine.protodata.java.JavaTypeName
import io.spine.protodata.java.MethodCall
import io.spine.string.camelCase
import io.spine.string.lowerCamelCase
import io.spine.tools.mc.java.base.isNotNested
import io.spine.tools.mc.java.base.root
import java.util.function.Function

private typealias FieldExtractor = Expression<Function<Message, Any>>
private typealias FieldComparator = Expression<Comparator<Any>>

/**
 * Builds a static Java field containing the [Comparator] for the given message.
 *
 * An example of the built comparator:
 *
 * ```java
 * private static final java.util.Comparator<Jogging> comparator =
 *     java.util.Comparator.comparing(Jogging::getStarted)
 *                         .thenComparing(Jogging::getFinished)
 *                         .thenComparing(Jogging::getDuration);
 * ```
 *
 * Please note the line breaks were added manually for reader's convenience.
 * The builder doesn't add them. It will be a single line of text.
 *
 * @param cls The message class to be used as comparator's generic parameter.
 * @param reversed If `true`, imposes the reverse of the natural ordering.
 *  If a comparator sorts objects in ascending order, reversed will sort them
 *  in descending order.
 */
internal class ComparatorBuilder(cls: PsiClass, private val reversed: Boolean = false) {

    private val message = cls.name!!
    private val instance = message.lowerCamelCase()
    private val fields = mutableListOf<Pair<FieldExtractor, FieldComparator?>>()

    /**
     * Builds a static `comparator` Java field in a text form.
     */
    fun build(): InitField<Comparator<Message>> {
        var comparator = MethodCall<Comparator<Message>>(
            ClassName("java.util", "Comparator"),
            "comparing",
            fields[0].asArgs()
        )
        for (i in 1 until fields.size) {
            comparator = comparator.chain("thenComparing", fields[i].asArgs())
        }
        if (reversed) {
            comparator = comparator.chain("reversed")
        }
        return InitField(
            modifiers = "private static final",
            type = JavaTypeName("java.util.Comparator<$message>"),
            "comparator",
            comparator
        )
    }

    /**
     * Adds the next comparing closure for the given field [path].
     *
     * For example, `joggingStartedEvent.emittedAt` field and the optional
     * `com.google.protobuf.util.Timestamps.comparator()` comparator for it.
     *
     * The comparator should be passed when the field type is neither primitive
     * nor comparable. It is a responsibility of the caller to check it in advance.
     * This builder is responsible only for building a string representation of
     * the comparator.
     *
     * @param path The path to the field.
     * @param comparator The optional comparator to be used for the field values.
     */
    fun comparingBy(path: FieldPath, comparator: Expression<Comparator<Any>>? = null) {
        val extractor = if (path.isNotNested) extractField(path.root) else extractNestedField(path)
        fields.add(extractor to comparator)
    }

    /**
     * Returns a method reference to the getter for the given [fieldName] in the [message].
     */
    private fun extractField(fieldName: String): Expression<Function<Message, Any>> =
        Expression("$message::${fieldName.toJavaGetter()}")

    /**
     * Builds a lambda key extractor for a nested field in the [message],
     * denoted by the given [path].
     */
    private fun extractNestedField(path: FieldPath): Expression<Function<Message, Any>> {
        val parts = path.fieldNameList
        val joined = parts.joinToString(".") { "${it.toJavaGetter()}()" }
        return Expression("($message $instance) -> $instance.$joined")
    }
}

/**
 * Converts this [String] with a Protobuf field name to a Java getter.
 *
 * For example, `my_best_field` will be converted to `getMyBestField`.
 *
 * The round brackets are omitted to allow this getter name to be used for composing
 * both a direct invocation and a method reference.
 *
 * For example, `getMyBestField()` and `::getMyBestField`.
 */
private fun String.toJavaGetter() = "get${camelCase()}"

private fun Pair<FieldExtractor, FieldComparator?>.asArgs() = toList().filterNotNull()
