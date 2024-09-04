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

internal class ComparatorBuilder(private val simpleClassName: String) {

    private val classInstance = simpleClassName.lowerCased
    private val closures = mutableListOf<String>()

    fun build(): String {
        var joinedClosures = "Comparator.comparing(${closures[0]})"
        for (i in 1 until closures.size) {
            joinedClosures += ".thenComparing(${closures[i]})"
        }
        return "private static final Comparator<$simpleClassName> comparator = $joinedClosures;"
    }

    fun comparingBy(path: FieldPath, comparator: String? = null) {
        val extractor = if (path.isNotNested) extractField(path) else extractNestedField(path)
        val closure = if (comparator == null) extractor else "$extractor, $comparator"
        closures.add(closure)
    }

    private fun extractField(fieldName: String): String = "$simpleClassName::${javaName(fieldName)}"

    private fun extractNestedField(path: FieldPath): String {
        val parts = path.split(".")
        val joined = parts.joinToString(".") { "${javaName(it)}()" }
        return "($simpleClassName $classInstance) -> $classInstance.$joined"
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

private val String.upperCased
    get() = replaceFirstChar { it.uppercase() }

private val String.lowerCased
    get() = replaceFirstChar { it.lowercase() }
