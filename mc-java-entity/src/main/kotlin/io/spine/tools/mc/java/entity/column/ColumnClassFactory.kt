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

import com.google.common.collect.ImmutableSet
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.PsiNameHelperImpl
import io.spine.protodata.Field
import io.spine.protodata.MessageType
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.file.toPsi
import io.spine.protodata.codegen.java.javaClassName
import io.spine.protodata.codegen.java.reference
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.type.TypeSystem
import io.spine.string.naturalizeEndings
import io.spine.tools.code.manifest.Version
import io.spine.tools.mc.entity.columns
import io.spine.tools.mc.java.entity.column.ColumnClassFactory.Companion.render
import io.spine.tools.psi.java.Environment
import io.spine.tools.psi.java.MetaLanguageSupport
import io.spine.tools.psi.java.PsiWrite.elementFactory
import io.spine.tools.psi.java.addFirst
import io.spine.tools.psi.java.createUtilityConstructor
import io.spine.tools.psi.java.makeFinal
import io.spine.tools.psi.java.makePublic
import io.spine.tools.psi.java.makeStatic
import io.spine.tools.psi.java.topLevelClass
import java.lang.String.format
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
    private val columnClass by lazy {
        elementFactory.createClass(CLASS_NAME)
    }
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
        @Suppress("TooGenericExceptionCaught")
        fun render(
            typeSystem: TypeSystem,
            file: SourceFile,
            type: MessageType
        ) {
            Environment.project
                .registerService(PsiManager::class.java, PsiManagerImpl::class.java)
            Environment.project
                .registerService(PsiNameHelper::class.java, PsiNameHelperImpl::class.java)

            MetaLanguageSupport.setUp()

            try {
                val header = typeSystem.findMessage(type.name)!!.second
                val entityStateClass = type.javaClassName(header)
                val psiJavaFile = file.toPsi()
                val topLevelClass = psiJavaFile.topLevelClass
                val factory = ColumnClassFactory(typeSystem, type, entityStateClass)
                val columnHolder = factory.create()
                topLevelClass.addLast(columnHolder)

                val updatedText = psiJavaFile.text
                val naturalized = updatedText.naturalizeEndings()
                file.overwrite(naturalized)
            } catch (e: Exception) {
                System.err.println(" ***** [ColumnFactory] Caught exception: `${e.message}`.")
                throw e
            }
        }
    }

    private fun create(): PsiClass {
        addAnnotation()
        addClassJavadoc()
        columnClass.makePublic().makeStatic().makeFinal()
        val createPrivateConstructor = elementFactory.createUtilityConstructor(columnClass)
        columnClass.addLast(createPrivateConstructor)
        addColumnMethods()
        addDefinitionsMethod()
        return columnClass
    }

    private fun addAnnotation() {
        val version = Version.fromManifestOf(this::class.java)

        @Suppress("EmptyClass")
        @Language("JAVA")
        val annotation = elementFactory.createAnnotationFromText(
            """
            @javax.annotation.Generated("by Spine Model Compiler (version: ${version.value}")
        """.trimIndent(), null
        )
        columnClass.addFirst(annotation)
    }

    private fun addClassJavadoc() {
        @Suppress("EmptyClass")
        @Language("JAVA")
        val classJavadoc = elementFactory.createDocCommentFromText("""
            /**
             * A listing of entity columns defined in $stateJavadocRef.
             *
             * <p>Use static methods of this class to access the columns of the entity
             * which can then be used for creating filters in a query.
             */
            """.trimIndent(), null
        )
        columnClass.addFirst(classJavadoc)
    }

    private fun addColumnMethods() {
        columns.forEach { column ->
            val accessor = ColumnAccessor(typeSystem, entityState, column, columnClass)
            columnClass.addLast(accessor.method())
        }
    }

    private fun addDefinitionsMethod() {
        val columnWildcard = columnType(entityState)
        @Suppress("EmptyClass")
        val accumulator = "result"
        val setRef = ImmutableSet::class.reference
        @Language("JAVA")
        val methodTemplate = """
            /**
             * Returns all the column definitions of $stateJavadocRef.
             */
            public static $setRef<$columnWildcard> definitions() {
              var $accumulator = new java.util.HashSet<$columnWildcard>();
              %s
              return $setRef.copyOf($accumulator);
            }                                
            """.trimIndent()
        val addingColumns = columns
            .map { "$accumulator.add(${columnMethodName(it)}());" }
            .joinToString(separator = "\n  ")
        val methodText = format(methodTemplate, addingColumns)
        val method = elementFactory.createMethodFromText(methodText, columnClass)
        columnClass.addLast(method)
    }
}

private fun PsiClass.addLast(element: PsiElement): PsiClass {
    val closingBrace = children.last()
    addBefore(element, closingBrace)
    return this
}
