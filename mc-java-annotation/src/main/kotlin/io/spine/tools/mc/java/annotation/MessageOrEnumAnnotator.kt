/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.tools.mc.java.annotation

import io.spine.base.EntityState
import io.spine.protodata.ProtoFileHeader
import io.spine.protodata.codegen.java.MessageOrEnumConvention
import io.spine.protodata.codegen.java.javaMultipleFiles
import io.spine.protodata.renderer.SourceFileSet
import io.spine.tools.mc.annotation.ApiOption
import io.spine.tools.mc.annotation.WithOptions

/**
 * An abstract base for annotators of message types and enums.
 *
 * This class defines the [convention] for the message types and enums.
 * It also implements filtering logic common for messages and enums in
 * [needsAnnotation] and [suitableFor] methods.
 */
internal sealed class MessageOrEnumAnnotator<T>(viewClass: Class<T>) :
    TypeAnnotator<T>(viewClass) where T : EntityState<*>, T : WithOptions {

    protected val convention by lazy {
        MessageOrEnumConvention(typeSystem!!)
    }

    /**
     * Tells if the given message type or enum needs to be annotated assuming the file header.
     *
     * If the file header tells having an outer Java class, options applied
     * at the file level and at message or enum level may semantically duplicate
     * each other.
     *
     * For example, if file options are `java_multiple_files = false` and `(internal_all) = true`,
     * there is no need to have `(internal_type) = true` on a message type.
     *
     * This method checks such semantic duplications and returns `false` if found.
     * Otherwise, returns `true`.
     */
    override fun needsAnnotation(apiOption: ApiOption, header: ProtoFileHeader): Boolean {
        val singleFile = !header.javaMultipleFiles()
        val alreadyInHeader = header.optionList.contains(apiOption.fileOption)
        return !(singleFile && alreadyInHeader)
    }

    override fun suitableFor(sources: SourceFileSet): Boolean =
        sources.outputRoot.endsWith("java")
}
