/*
 * Copyright 2023, TeamDev. All rights reserved.
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
import io.spine.protodata.codegen.java.ClassOrEnumName
import io.spine.protodata.codegen.java.JavaRenderer
import io.spine.protodata.codegen.java.annotation.TypeAnnotation
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.tools.mc.annotation.ApiOption.Companion.findMatching
import io.spine.tools.mc.annotation.WithOptions
import io.spine.tools.mc.annotation.optionList

/**
 * An abstract base for annotation renderers.
 *
 * @param T the type of the view state which contains information about annotated types.
 */
internal sealed class AnnotationRenderer<T>(
    private val viewClass: Class<T>
) : JavaRenderer() where T : EntityState<*>, T : WithOptions {

    protected lateinit var sources: SourceFileSet

    /**
     * Tells if the given source file set is suitable for this renderer.
     *
     * @see <a href="https://github.com/SpineEventEngine/ProtoData/issues/150">ProtoData issue</a>
     */
    protected abstract fun suitableFor(sources: SourceFileSet): Boolean

    final override fun render(sources: SourceFileSet) {
        if (suitableFor(sources)) {
            this.sources = sources
            doRender()
        }
    }

    private fun doRender() {
        val annotated: Set<T> = select(viewClass).all()
        annotated.forEach {
            annotate(it)
        }
    }

    @OverridingMethodsMustInvokeSuper
    protected open fun annotate(state: T) {
        // Handle the case of having the option defined on a file level,
        // and at a type level too.
        val options = state.optionList.toSet()
        options.mapNotNull { findMatching(it) }
            .forEach { apiOption ->
                annotateType(state, apiOption.annotationClass)
            }
    }

    abstract fun annotateType(state: T, annotationClass: Class<out Annotation>)
}

internal open class ApiTypeAnnotation<T : Annotation>(
    subject: ClassOrEnumName,
    annotationClass: Class<T>
) :
    TypeAnnotation<T>(annotationClass, subject) {

    override fun renderAnnotationArguments(file: SourceFile): String = ""
}
