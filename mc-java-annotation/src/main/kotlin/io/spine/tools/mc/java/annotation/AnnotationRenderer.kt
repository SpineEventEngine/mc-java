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

import io.spine.base.EntityState
import io.spine.protodata.codegen.java.JavaRenderer
import io.spine.protodata.renderer.SourceFileSet

internal sealed class AnnotationRenderer<T: EntityState<*>>(
    private val viewClass: Class<T>
): JavaRenderer() {

    // See https://github.com/SpineEventEngine/ProtoData/issues/150
    final override fun render(sources: SourceFileSet) {
        if (handlesJavaOrGprc(sources)) {
            doRender(sources)
        }
    }

    private fun doRender(sources: SourceFileSet) {
        val annotated: Set<T> = select(viewClass).all()
        annotated.forEach {
            annotate(sources, it)
        }
    }

    abstract fun annotate(sources: SourceFileSet, state: T)

    private fun handlesJavaOrGprc(sources: SourceFileSet): Boolean {
        val outputRoot = sources.outputRoot
        return outputRoot.endsWith("java") || outputRoot.endsWith("grpc")
    }
}
