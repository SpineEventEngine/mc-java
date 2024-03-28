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
import com.intellij.psi.PsiMethod
import io.spine.logging.WithLogging
import io.spine.protodata.Field
import io.spine.protodata.MessageType
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.file.toPsi
import io.spine.protodata.java.javaClassName
import io.spine.protodata.java.reference
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.type.TypeSystem
import io.spine.tools.code.manifest.Version
import io.spine.tools.mc.entity.columns
import io.spine.tools.mc.java.entity.column.ColumnClassFactory.Companion.render
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addFirst
import io.spine.tools.psi.java.addLast
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
@Suppress("EmptyClass") // ... to avoid false positives for `@Language` strings.
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
    private val stateJavadocRef: String = "{@code ${entityState.simpleName}}"

    companion object: WithLogging {

        /**
         * The name of the created class.
         */
        const val CLASS_NAME = "Column"

        /**
         * The name of the method for obtaining all the columns.
         *
         * We use `buildString` instead of a plain literal to avoid the `Missing identifier`
         * warning in IDEA.
         */
        val DEFINITIONS_METHOD = buildString {
            append("definitions")
        }

        /**
         * Adds a nested class called [Column][CLASS_NAME] into the top class of the given [file].
         *
         * The class provides API for obtaining columns for the given `EntityState` [type].
         * The `Column` class is `public static` and stateless.
         * It serves as a DSL for calling `public static` methods for obtaining
         * entity state [columns][io.spine.query.EntityColumn].
         *
         * Since the `Column` class is not meant to be instantiated, a private parameterless
         * constructor is generated.
         *
         * In addition to methods for obtaining individual columns, a [method][DEFINITIONS_METHOD]
         * for obtaining all the columns is also generated.
         *
         * @param typeSystem
         *         the type system used for resolving field types.
         * @param file
         *         the Java file to add the `Column` class.
         * @param type
         *         the type of the `EntityState` message.
         */
        @Suppress("TooGenericExceptionCaught") // ... to log diagnostic.
        fun render(
            typeSystem: TypeSystem,
            file: SourceFile,
            type: MessageType
        ) {
            try {
                val header = typeSystem.findMessage(type.name)!!.second
                val entityStateClass = type.javaClassName(header)
                val psiJavaFile = file.toPsi()
                val topLevelClass = psiJavaFile.topLevelClass
                val factory = ColumnClassFactory(typeSystem, type, entityStateClass)
                val columnHolder = factory.create()
                topLevelClass.addLast(columnHolder)

                val updatedText = psiJavaFile.text
                file.overwrite(updatedText)
            } catch (e: Throwable) {
                logger.atError().log {
                    "Caught exception while generating `Column` class: `${e.message}`."
                }
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
        val version = Version.fromManifestOf(this::class.java).value
        @Language("JAVA")
        val annotation = elementFactory.createAnnotationFromText(
            """
            @javax.annotation.Generated("by Spine Model Compiler (version: $version)")
            """.trimIndent(), null
        )
        columnClass.addFirst(annotation)
    }

    private fun addClassJavadoc() {
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
        val method = DefinitionsMethod().create()
        columnClass.addLast(method)
    }

    /**
     * Method object for creating [definitions][DEFINITIONS_METHOD] method.
     */
    private inner class DefinitionsMethod {

        /** The generic type which matches all the columns of this entity state. */
        private val columnWildcard = columnType(entityState)

        /**
         * The name of the variable to collect all column definitions
         *
         * We use `buildString` instead of a plain literal to avoid the `Missing identifier`
         * warning in IDEA.
         */
        private val accumulator: String = buildString {
            append("columns")
        }

        /** The type which is returned by the method. */
        private val resultSet: String = ImmutableSet::class.reference

        /** The piece of method body which adds columns to [accumulator]. */
        private val addingColumns: String by lazy {
            columns
                .map { column -> columnMethodName(column) }
                .joinToString(separator = "\n  ") { method ->
                    "$accumulator.add($method());"
                }
        }

        /** Builds the full text of the method. */
        private val methodText: String by lazy {
            @Language("JAVA")
            @Suppress("DanglingJavadoc") // to avoid false positive warning.
            val methodTemplate = """
            /**
             * Returns all the column definitions of $stateJavadocRef.
             */
            public static $resultSet<$columnWildcard> $DEFINITIONS_METHOD() {
              var $accumulator = new java.util.HashSet<$columnWildcard>();
              %s
              return $resultSet.copyOf($accumulator);
            }                                
            """.trimIndent()
            format(methodTemplate, addingColumns)
        }

        fun create(): PsiMethod {
            val method = elementFactory.createMethodFromText(methodText, columnClass)
            return method
        }
    }
}
