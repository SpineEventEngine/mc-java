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

/**
 * Builds a `comparator` for the given message.
 *
 * @param psiCls The message class to be used as comparator's generic parameter.
 */
internal class ComparatorBuilder(psiCls: PsiClass) {

    private val message = psiCls.name!!
    private val instance = message.lowerCased
    private val closures = mutableListOf<String>()
    private var reversed = false

    /**
     * Builds a `comparator` field in a text form.
     */
    fun build(): String {
        var joinedClosures = "java.util.Comparator.comparing(${closures[0]})"
        for (i in 1 until closures.size) {
            joinedClosures += ".thenComparing(${closures[i]})"
        }
        if (reversed) {
            joinedClosures += ".reversed()"
        }
        return "private static final java.util.Comparator<$message> comparator = $joinedClosures;"
    }

    /**
     * Adds a field to participate in comparison.
     *
     * For example, `event.emittedAt` field, may need
     * `com.google.protobuf.util.Timestamps.comparator()` comparator.
     *
     * @param path The path to the field.
     * @param comparator The optional comparator to be used for the field's value.
     */
    fun comparingBy(path: FieldPath, comparator: String? = null) {
        val extractor = if (path.isNotNested) extractField(path) else extractNestedField(path)
        val closure = if (comparator == null) extractor else "$extractor, $comparator"
        closures.add(closure)
    }

    fun reversed() {
        reversed = true
    }

    /**
     * Returns a reference to the getter for the given [fieldName] in the [message].
     */
    private fun extractField(fieldName: String): String = "$message::${javaName(fieldName)}"

    /**
     * Builds a lambda key extractor for a nested field in the [message],
     * denoted by the given [path].
     */
    private fun extractNestedField(path: FieldPath): String {
        val parts = path.split(".")
        val joined = parts.joinToString(".") { "${javaName(it)}()" }
        return "($message $instance) -> $instance.$joined"
    }
}

/**
 * Converts the given [protoFieldName] to Java field name.
 *
 * For example, `my_best_field` will be converted to `myBestField`.
 */
private fun javaName(protoFieldName: String): String {
    val parts = protoFieldName.split("_")
    val joined = parts.joinToString("") { it.upperCased }
    return "get$joined"
}
