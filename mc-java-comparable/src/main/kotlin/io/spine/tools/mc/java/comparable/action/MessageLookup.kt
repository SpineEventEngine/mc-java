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

import io.spine.protodata.CodegenContext
import io.spine.protodata.MessageType
import io.spine.protodata.ProtobufDependency
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.TypeName

/**
 * Looks for [MessageType] by [TypeName] in the given codegen [context].
 */
internal class MessageLookup(private val context: CodegenContext) {

    /**
     * Queries [MessageType] by the given [typeName] in [CodegenContext].
     *
     * Firstly, the method attempts to find it within the generated Proto files,
     * then in their dependencies. It throws ISE if the message is not found.
     * In this case, we would not be able to know whether the message can participate
     * in comparison (has `compare_by` option or the default comparator like `Timestamps`).
     */
    fun query(typeName: TypeName): MessageType {
        val typeUrl = typeName.typeUrl
        return fromOurProtos(typeUrl)
            ?: fromDependencies(typeUrl)
            ?: error("`$typeUrl` not found in the passed Proto files and their dependencies.")
    }

    private fun fromOurProtos(typeUrl: String): MessageType? =
        context.select(ProtobufSourceFile::class.java).all()
            .firstOrNull { it.containsType(typeUrl) }
            ?.typeMap?.get(typeUrl)

    private fun fromDependencies(typeUrl: String): MessageType? =
        context.select(ProtobufDependency::class.java).all()
            .firstOrNull { it.source.containsType(typeUrl) }
            ?.source?.typeMap?.get(typeUrl)
}
