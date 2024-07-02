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

package io.spine.tools.mc.java.field

import com.intellij.psi.PsiClass
import io.spine.protodata.CodegenContext
import io.spine.protodata.Field.CardinalityCase.SINGLE
import io.spine.protodata.MessageType
import io.spine.protodata.MessageTypeDependencies
import io.spine.protodata.java.ClassName
import io.spine.tools.mc.java.NestedClassAction
import io.spine.tools.mc.java.field.FieldClass.Companion.NAME
import io.spine.tools.psi.java.addLast
import org.intellij.lang.annotations.Language

/**
 * Creates a nested class called [`Field`][NAME] under a Java class generated for
 * the given message [type].
 *
 * @param type
 *         the message type for the Java code of which to generate the nested class.
 * @param fieldSupertype
 *         the class name for the supertype of generated nested field classes, e.g.,
 *         [io.spine.base.EventMessageField] or [io.spine.query.EntityStateField].
 */
public class FieldClass(
    type: MessageType,
    private val fieldSupertype: ClassName,
    context: CodegenContext
) : NestedClassAction(type, NAME, context) {

    public companion object {

        /**
         * The name of the generated Java class.
         */
        public const val NAME: String = "Field"
    }

    @Language("JAVA") @Suppress("EmptyClass")
    override fun classJavadoc(): String = """
        /**
         * The listing of $messageJavadocRef fields to be used for creating a subscription filter.
         *
         * <p>Please use static methods of this class to access top-level fields of
         * the entity state type.
         * 
         * <p>Nested fields can be accessed using the values returned by the top-level
         * field accessors, through method chaining.
         */ 
        """.trimIndent()

    override fun tuneClass() {
        cls.addTopLevelFieldMethods()
        cls.addFieldClasses()
    }

    private fun PsiClass.addTopLevelFieldMethods() {
        type.fieldList.forEach {
            val accessor = TopLevelFieldAccessor(it, fieldSupertype, typeSystem!!)
            addLast(accessor.method())
        }
    }

    private fun PsiClass.addFieldClasses() {
        val nestedFieldTypes =
            MessageTypeDependencies(type, cardinality = SINGLE, typeSystem!!).asSet()
        nestedFieldTypes.forEach {
            val fld = MessageTypedField(it, fieldSupertype, typeSystem!!)
            val messageTypeField = fld.createClass()
            addLast(messageTypeField)
        }
    }
}
