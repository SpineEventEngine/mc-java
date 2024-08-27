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

package io.spine.tools.mc.java.comparable

import io.spine.protodata.MessageType
import io.spine.protodata.java.JavaRenderer
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.tools.code.Java
import io.spine.tools.psi.java.execute

internal class ComparableRenderer : JavaRenderer() {

    override fun render(sources: SourceFileSet) {
        val relevant = sources.hasJavaRoot
        if (!relevant) {
            return
        }
        val views = findViews()
        views.forEach { view ->
            val type = view.type
            val sourceFile = sources.javaFileOf(type)
            execute {
                doRender(type, sourceFile)
            }
        }
    }

    private fun doRender(type: MessageType, sourceFile: SourceFile<Java>) {
        val actions = listOf(
            ImplementComparable(type, sourceFile, context!!),
            AddCompareToMethod(type, sourceFile, context!!),
        )
        actions.forEach {
            it.render()
        }
    }

    private fun findViews(): Set<ComparableMessage> {
        val found = select(ComparableMessage::class.java).all()
        return found
    }
}
