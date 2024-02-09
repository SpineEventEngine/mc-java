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

package io.spine.tools.mc.java.entity.column

import io.spine.protodata.MessageType
import io.spine.protodata.codegen.java.JavaRenderer
import io.spine.protodata.codegen.java.file.hasJavaOutput
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.settings.loadSettings
import io.spine.tools.mc.entity.DiscoveredEntities
import io.spine.tools.mc.entity.EntityPluginComponent
import io.spine.tools.mc.entity.columns
import io.spine.tools.mc.java.codegen.Entities
import io.spine.tools.mc.java.entity.column.ColumnClassFactory.Companion.renderColumns
import io.spine.tools.psi.java.PsiWrite

/**
 * Renders classes named `Columns` which are nested into messages classes that
 * are marked as entity states.
 *
 * @see io.spine.tools.mc.entity.EntityDiscoveryProcess
 */
internal class ColumnRenderer : JavaRenderer(), EntityPluginComponent {

    private val settings: Entities by lazy {
        loadSettings()
    }

    override fun render(sources: SourceFileSet) {
        val relevant = sources.hasJavaOutput && settings.generateQueries
        if (!relevant) {
            return
        }
        val entityStates = foundEntityStates()
        entityStates
            .filter { it.columns.isNotEmpty() }
            .forEach {
                val sourceFile = it.sourceFileIn(sources)
                val className = classNameOf(it.name, it.file)
                PsiWrite.execute {
                    renderColumns(it, sourceFile, className)
                }
            }
    }

    /**
     * Obtains entity state types declared in all proto files.
     */
    private fun foundEntityStates(): List<MessageType> {
        val discoveredEntities = select<DiscoveredEntities>().all()
        val result = discoveredEntities.flatMap { it.typeList }
        return result
    }

    private fun MessageType.sourceFileIn(sources: SourceFileSet): SourceFile {
        val javaFile = javaFileOf(type = name, declaredIn = file)
        val sourceFile = sources.find(javaFile)
        check(sourceFile != null) {
            "Unable to locate the file `$sourceFile` in the source set `$sources`."
        }
        return sourceFile
    }
}


