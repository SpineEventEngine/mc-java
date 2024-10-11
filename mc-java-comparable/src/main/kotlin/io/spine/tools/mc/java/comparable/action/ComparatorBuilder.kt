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

import com.intellij.psi.PsiClass
import io.spine.base.FieldPath
import io.spine.string.camelCase
import io.spine.string.lowerCamelCase

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
 * @param descending If true, the default order is reversed.
 */
internal class ComparatorBuilder(cls: PsiClass, private val descending: Boolean = false) {

    private val message = cls.name!!
    private val instance = message.lowerCamelCase()
    private val closures = mutableListOf<String>()

    /**
     * Builds a static `comparator` Java field in a text form.
     */
    fun build(): String {
        var joinedClosures = "java.util.Comparator.comparing(${closures[0]})"
        for (i in 1 until closures.size) {
            joinedClosures += ".thenComparing(${closures[i]})"
        }
        if (descending) {
            joinedClosures += ".reversed()"
        }
        return "private static final java.util.Comparator<$message> comparator = $joinedClosures;"
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
    fun comparingBy(path: FieldPath, comparator: String? = null) {
        val extractor = if (path.isNotNested) extractField(path.root) else extractNestedField(path)
        val closure = if (comparator == null) extractor else "$extractor, $comparator"
        closures.add(closure)
    }

    /**
     * Returns a method reference to the getter for the given [fieldName] in the [message].
     */
    private fun extractField(fieldName: String): String = "$message::${fieldName.toJavaGetter()}"

    /**
     * Builds a lambda key extractor for a nested field in the [message],
     * denoted by the given [path].
     */
    private fun extractNestedField(path: FieldPath): String {
        val parts = path.fieldNameList
        val joined = parts.joinToString(".") { "${it.toJavaGetter()}()" }
        return "($message $instance) -> $instance.$joined"
    }
}

/**
 * Converts this [String] with a Proto field name to a Java getter.
 *
 * For example, `my_best_field` will be converted to `getMyBestField`.
 *
 * The round brackets are omitted to allow this getter name to be used for composing
 * both a direct invocation and a method reference.
 *
 * For example, `getMyBestField()` and `::getMyBestField`.
 */
private fun String.toJavaGetter() = "get${camelCase()}"
