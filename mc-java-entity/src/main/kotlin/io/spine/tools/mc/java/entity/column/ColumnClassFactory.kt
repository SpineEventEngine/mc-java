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
import com.intellij.psi.PsiMethod
import io.spine.logging.WithLogging
import io.spine.protodata.Field
import io.spine.protodata.MessageType
import io.spine.protodata.columns
import io.spine.protodata.java.reference
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.type.TypeSystem
import io.spine.tools.mc.java.entity.NestedClassFactory
import io.spine.tools.mc.java.entity.column.ColumnClassFactory.Companion.CLASS_NAME
import io.spine.tools.mc.java.entity.column.ColumnClassFactory.Companion.DEFINITIONS_METHOD
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addFirst
import io.spine.tools.psi.java.addLast
import java.lang.String.format
import org.intellij.lang.annotations.Language

/**
 * Creates a class called [Column][CLASS_NAME] and nests it under an entity state class.
 *
 * The class provides API for obtaining columns for given `EntityState` [type].
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
 * @param type
 *         the type of the `EntityState` message.
 * @param typeSystem
 *         the type system used for resolving field types.
 * @see render
 */
@Suppress("EmptyClass") // ... to avoid false positives for `@Language` strings.
internal class ColumnClassFactory(
    type: MessageType,
    typeSystem: TypeSystem
) : NestedClassFactory(type, CLASS_NAME, typeSystem) {

    private val columnClass = nestedClass
    private val columns: List<Field> = type.columns

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
         * Adds a nested class the top class of the given [file].
         *
         * @param type
         *         the type of the `EntityState` message.
         * @param file
         *         the Java file to add the `Column` class.
         * @param typeSystem
         *         the type system used for resolving field types.
         */
        fun render(
            type: MessageType,
            file: SourceFile,
            typeSystem: TypeSystem
        ) {
            val factory = ColumnClassFactory(type, typeSystem)
            factory.render(file)
        }
    }

    @Language("JAVA")
    override fun classJavadoc(): String = """
        /**
         * A listing of entity columns defined in $stateJavadocRef.
         *
         * <p>Use static methods of this class to access the columns of the entity
         * which can then be used for creating filters in a query.
         */
        """.trimIndent()

    override fun tuneClass() {
        addColumnMethods()
        addDefinitionsMethod()
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
