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

import io.spine.protodata.ast.Field
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.field

/**
 * Looks for [MessageType] denoted by [FieldPath].
 *
 * @param [messages] Lookup of all messages participating in codegen.
 */
internal class FieldLookup(private val messages: MessageLookup) {

    /**
     * Resolves [Field] denoted by the given field [path], relatively to
     * the given [rootMessage].
     *
     * For example, given the following message:
     *
     * ```proto
     * message Citizen {
     *     option (compare_by) = {
     *         field: "passport.first_name"
     *     };
     *    Passport passport = 1;
     * ```
     *
     * `passport.first_name` is a field path. `Citizen` is the root message,
     * in respect to which the path will be resolved. The resulting `Field`
     * will describe `Passport.first_name`.
     *
     * @param path The field path. Can be nested.
     * @param rootMessage The message, in respect to which the path will be resolved.
     */
    fun resolve(path: FieldPath, rootMessage: MessageType): Field =
        if (path.isNotNested) {
            rootMessage.field(path)
        } else {
            searchRecursively(path, rootMessage)
        }

    private fun searchRecursively(path: FieldPath, message: MessageType): Field {
        if (path.isNotNested) {
            return message.field(path)
        }

        val currentFieldName = path.substringBefore(".")
        val currentField = message.field(currentFieldName)

        val remainingFields = path.substringAfter(".")
        val nextMessage = messages.query(currentField.type.message)
        return searchRecursively(remainingFields, nextMessage)
    }
}
