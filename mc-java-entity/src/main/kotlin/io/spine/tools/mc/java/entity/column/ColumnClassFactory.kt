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

import com.intellij.psi.PsiClass
import io.spine.protodata.Field
import io.spine.protodata.MessageType
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.file.toPsi
import io.spine.protodata.codegen.java.javaClassName
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.type.TypeSystem
import io.spine.string.joinByLines
import io.spine.tools.code.manifest.Version
import io.spine.tools.mc.entity.columns
import io.spine.tools.mc.java.entity.column.ColumnClassFactory.Companion.render
import io.spine.tools.psi.java.PsiWrite.elementFactory
import io.spine.tools.psi.java.addFirst
import io.spine.tools.psi.java.createPrivateConstructor
import io.spine.tools.psi.java.makeFinal
import io.spine.tools.psi.java.makePublic
import io.spine.tools.psi.java.makeStatic
import io.spine.tools.psi.java.topLevelClass
import org.intellij.lang.annotations.Language

/**
 * Creates a class called `Column` and nest it under the top level entity state class.
 *
 * @see render
 */
internal class ColumnClassFactory(
    private val typeSystem: TypeSystem,
    type: MessageType,
    private val entityState: ClassName
) {
    private val columnClass = elementFactory.createClass(CLASS_NAME)
    private val columns: List<Field> = type.columns

    /**
     * Reference to [entityState] made in Javadoc.
     */
    @Suppress("EmptyClass")
    private val stateJavadocRef: String = "{@code ${entityState.simpleName}}"

    companion object {

        /**
         * The name of the created class.
         */
        const val CLASS_NAME = "Column"

        /**
         * Adds a `public static class` [Column][CLASS_NAME] which provides column API
         * for the given [type].
         */
        fun render(
            typeSystem: TypeSystem,
            file: SourceFile,
            type: MessageType
        ) {
            val header = typeSystem.findMessage(type.name)!!.second
            val entityStateClass = type.javaClassName(header)
            val psiJavaFile = file.toPsi()
            val topLevelClass = psiJavaFile.topLevelClass
            val factory = ColumnClassFactory(typeSystem, type, entityStateClass)
            val columnHolder = factory.create()
            topLevelClass.add(columnHolder)
        }
    }

    private fun create(): PsiClass {
        addAnnotation()
        addClassJavadoc()
        columnClass.makePublic().makeStatic().makeFinal()
        columnClass.add(elementFactory.createPrivateConstructor(columnClass))
        addColumnMethods()
        addDefinitionsMethod()
        return columnClass
    }

    private fun addAnnotation() {
        val version = Version.fromManifestOf(this::class.java)
        @Suppress("EmptyClass")
        @Language("JAVA")
        val annotation = elementFactory.createAnnotationFromText("""
            @javax.annotation.Generated("by Spine Model Compiler (version: ${version.value}")
        """.trimIndent(), null)
        columnClass.addFirst(annotation)
    }

    private fun addClassJavadoc() {
        @Suppress("EmptyClass")
        @Language("JAVA")
        val classJavadoc = elementFactory.createCommentFromText("""
            /**
             * A listing of entity columns defined in $stateJavadocRef.
             *
             * <p>Use static methods of this class to access the columns of the entity
             * which can then be used for creating filters in a query.
             */
        """.trimIndent(), null)
        columnClass.addFirst(classJavadoc)
    }

    private fun addColumnMethods() {
        columns.forEach { column ->
            val accessor = ColumnAccessor(typeSystem, entityState, column, columnClass)
            columnClass.add(accessor.method())
        }
    }

    private fun addDefinitionsMethod() {
        val columnWildcard = columnType(entityState)
        @Suppress("EmptyClass")
        val accumulator = "result"
        val addingColumns = columns.map { "$accumulator.add(${columnMethodName(it)};" }
            .joinByLines()
        @Language("JAVA")
        val method = elementFactory.createMethodFromText("""
            /**
             * Returns all the column definitions of $stateJavadocRef.
             */
            public static $columnWildcard definitions() {
              var $accumulator = new java.util.HashSet<$columnWildcard>();
              $addingColumns
              return com.google.common.collect.ImmutableSet.copyOf($accumulator);
            }                                
            """.trimIndent(), columnClass
        )
        columnClass.add(method)
    }
}
