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

package io.spine.tools.mc.java.entity.query

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import io.spine.protodata.MessageType
import io.spine.protodata.columns
import io.spine.protodata.java.javaCase
import io.spine.query.EntityQueryBuilder
import io.spine.tools.java.reference
import io.spine.tools.mc.java.entity.EntityPlugin.Companion.QUERY_BUILDER_CLASS_NAME
import io.spine.tools.mc.java.entity.EntityPlugin.Companion.QUERY_CLASS_NAME
import io.spine.tools.mc.java.entity.idField
import io.spine.tools.mc.java.settings.Entities
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addSuperclass
import io.spine.tools.psi.java.createClassReference
import org.intellij.lang.annotations.Language

/**
 * Creates a [QueryBuilder][QUERY_BUILDER_CLASS_NAME] class nested under an entity state class.
 *
 * @see QueryClass
 */
internal class QueryBuilderClass(
    type: MessageType,
    settings: Entities
) : QuerySupportClass(QUERY_BUILDER_CLASS_NAME, type, settings) {

    /**
     * The value used for brevity when referencing in the generated code.
     */
    private val query = QUERY_CLASS_NAME

    @Language("JAVA") @Suppress("EmptyClass")
    override fun classJavadoc(): String = """
        /**
         * A builder for the queries for the {@link $stateType} entity state.
         * 
         * @see $query
         */       
        """.trimIndent()

    /**
     * Creates the private constructor which calls `super()` with two parameters:
     *  1. The class of the entity state identifiers.
     *  2. The entity state class.
     */
    override fun createConstructor(cls: PsiClass): PsiMethod {
        val ctor = elementFactory.createMethodFromText("""
            private $className() {
                super($idType.class, $stateType.class);
            }
            """.trimIndent(), cls
        )
        return ctor
    }

    override fun tuneClass() {
        cls.run {
            extendEntityQueryBuilder()
            addIdMethod()
            addColumnsMethod()
            addThisRefMethod()
            addBuildMethod()
        }
    }

    /**
     * Makes the class extend [EntityQueryBuilder].
     *
     * The generic parameters are:
     * 1) The type of the [first][idField] entity state type field.
     * 2) The type of the entity state.
     * 3) The type of this query builder class.
     * 4) The type of the query class.
     */
    private fun PsiClass.extendEntityQueryBuilder() {
        val superClass = elementFactory.createClassReference(
            cls,
            EntityQueryBuilder::class.java.reference,
            // Generic parameters:
            idType, stateType, this.name!!, query
        )
        addSuperclass(superClass)
    }

    private fun PsiClass.addIdMethod() {
        val idMethod = IdMethod(
            entityStateClass,
            queryBuilderClass = this,
            methodName = idField.name.javaCase(),
            idType,
        )
        idMethod.render()
    }

    private fun PsiClass.addColumnsMethod() {
        val columns = type.columns
        columns.forEach {
            val column = QueryColumn(entityStateClass, it, queryBuilder = this, typeSystem!!)
            column.render()
        }
    }

    private fun PsiClass.addThisRefMethod() =
        ThisRefMethod(queryBuilder = this).run {
            render()
        }

    private fun PsiClass.addBuildMethod() =
        BuildMethod(queryBuilder = this).run {
            render()
        }
}
