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

package io.spine.tools.mc.java.entity.field

import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.protodata.Field
import io.spine.protodata.Field.CardinalityCase.SINGLE
import io.spine.protodata.isMessage
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.javaCase
import io.spine.protodata.java.reference
import io.spine.protodata.toMessageType
import io.spine.protodata.type.TypeSystem
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory
import org.intellij.lang.annotations.Language

internal abstract class FieldAccessor(
    private val field: Field,
    private val fieldSupertype: ClassName,
    private val typeSystem: TypeSystem
) {

    abstract val modifiers: String

    abstract val methodBody: String

    fun method(): PsiMethod {
        @Language("JAVA") @Suppress("EmptyClass")
        val method = elementFactory.createMethodFromText("""
            $modifiers $returnType $methodName() {
                $methodBody
            }  
            """.trimIndent(), null
        )
        method.addFirst(javadoc)
        return method
    }

    protected val fieldName: String = field.name.value

    protected val returnType: String by lazy {
        if (shouldExposeNestedFields) {
            nestedFieldsContainerType()
        } else {
            simpleFieldType
        }
    }

    private val shouldExposeNestedFields: Boolean =
        field.isMessage && field.cardinalityCase == SINGLE

    private val simpleFieldType: String by lazy {
        fieldSupertype.canonical
    }

    private val methodName: String by lazy {
        field.name.javaCase()
    }

    private val javadoc: PsiDocComment by lazy {
        FieldAccessorDoc(field, typeSystem).javadoc()
    }

    private fun nestedFieldsContainerType(): String {
        check(field.isMessage)
        val fieldTypeName = field.type.toMessageType(typeSystem).name.simpleName
        val containerClassName = fieldTypeName + "Field"
        return containerClassName
    }
}

internal class TopLevelFieldAccessor(
    field: Field,
    fieldSupertype: ClassName,
    typeSystem: TypeSystem
) : FieldAccessor(field, fieldSupertype, typeSystem) {

    override val modifiers: String = "public static"

    override val methodBody: String by lazy {
        val fieldClass = io.spine.base.Field::class.java.reference
        @Language("JAVA") @Suppress("EmptyClass")
        val result = """
            return new $returnType($fieldClass.named("$fieldName"));            
            """.trimIndent()
        result
    }
}

internal class NestedFieldAccessor(
    field: Field,
    fieldSupertype: ClassName,
    typeSystem: TypeSystem
) : FieldAccessor(field, fieldSupertype, typeSystem) {

    override val modifiers: String = "public"

    override val methodBody: String by lazy {
        @Language("JAVA") @Suppress("EmptyClass")
        val result = """
            return new $returnType(getField().nested("$fieldName"));                
            """.trimIndent()
        result
    }
}
