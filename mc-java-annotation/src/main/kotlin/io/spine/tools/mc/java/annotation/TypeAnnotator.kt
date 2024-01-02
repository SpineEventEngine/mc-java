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

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import io.spine.base.EntityState
import io.spine.protodata.ProtoFileHeader
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.renderer.Renderer
import io.spine.tools.mc.annotation.ApiOption
import io.spine.tools.mc.annotation.WithOptions
import io.spine.tools.mc.annotation.file
import io.spine.tools.mc.annotation.optionList

/**
 * Adds annotations to the types in the generated code.
 */
internal abstract class TypeAnnotator<T>(
    viewClass: Class<T>
): Annotator<T>(viewClass) where T : EntityState<*>, T : WithOptions {

    @OverridingMethodsMustInvokeSuper
    override fun annotate(view: T) {
        view.optionList
            .mapNotNull { ApiOption.findMatching(it) }
            .filter {
                val header = findHeaderFor(view)
                needsAnnotation(it, header)
            }
            .map { it.annotationClass }
            .forEach {
                annotateType(view, it)
            }
    }

    /**
     * Tells if the type needs an annotation, assuming the options set directly, and
     * indirectly via the file header.
     */
    protected abstract fun needsAnnotation(apiOption: ApiOption, header: ProtoFileHeader): Boolean

    /**
     * Adds the annotation to the type.
     */
    abstract fun annotateType(view: T, annotationClass: Class<out Annotation>)
}

private fun <T> Renderer<*>.findHeaderFor(view: T): ProtoFileHeader
        where T : EntityState<*>, T : WithOptions {
    val protoFile = view.file
    val fileHeader = select<ProtobufSourceFile>().findById(protoFile)?.header
    check(fileHeader != null) {
        "Unable to find `${ProtobufSourceFile::class.java.simpleName}`" +
                " file: `${protoFile.path}`, " +
                " view: `${view}`."
    }
    return fileHeader
}
