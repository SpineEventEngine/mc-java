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

import io.spine.option.CompareByOption

/**
 * Builds a `private static final Comparator<Message> comparator` field to be inserted
 * by [AddComparator] action.
 */
internal class ComparatorBuilder {

    fun composeAsText(messageName: String, option: CompareByOption): String {
        val fields = option.fieldList.iterator()
        val declaration = buildString {
            append("Comparator.comparing(${closure(messageName, fields.next())})")
            while (fields.hasNext()) {
                append(".thenComparing(${closure(messageName, fields.next())})")
            }
        }
        return "private static final Comparator<$messageName> comparator = $declaration;"
    }

    private fun closure(outerMsg: String, field: String): String {
        val instance = outerMsg.lowerCased
        return if (field.contains(".")) {
            val parts = field.split(".")
            val joined = parts.joinToString(".") { "${toJavaGetter(it)}()" }
            "($outerMsg $instance) -> $instance.$joined"
        } else {
            "$outerMsg::${toJavaGetter(field)}"
        }
    }
}

private fun toJavaGetter(protobufFieldName: String): String {
    val parts = protobufFieldName.split("_")
    val joined = parts.joinToString("") { it.upperCased }
    return "get$joined"
}

private val String.upperCased
    get() = replaceFirstChar { it.uppercase() }

private val String.lowerCased
    get() = replaceFirstChar { it.lowercase() }
