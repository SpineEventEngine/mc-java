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

import io.spine.protodata.ast.File
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.ProtobufDependency
import io.spine.protodata.ast.ProtobufSourceFile
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.javaClassName

/**
 * Looks for [Class] by [MessageType] in the given codegen [context].
 */
internal class ClassLookup(private val context: CodegenContext) {

    fun query(type: MessageType): Class<*>? {
        val file = type.file
        val protoFile = fromOurProtos(file)
            ?: fromDependencies(file)
            ?: error("The requested `$file` not found.")
        val className = type.javaClassName(protoFile.header)
        return findClass(className)
    }

    // Anyway, it makes little sense to have a custom comparator for messages
    // that are now being generated. It is just impossible.
    private fun findClass(name: ClassName) = try {
        Class.forName(name.canonical)
    } catch (e: ClassNotFoundException) {
        null
    }

    private fun fromOurProtos(file: File): ProtobufSourceFile? =
        context.select(ProtobufSourceFile::class.java)
            .findById(file)

    private fun fromDependencies(file: File): ProtobufSourceFile? =
        context.select(ProtobufDependency::class.java)
            .findById(file)
            ?.source
}
