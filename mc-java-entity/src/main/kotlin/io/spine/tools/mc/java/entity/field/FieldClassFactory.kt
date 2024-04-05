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

import io.spine.base.SubscribableField
import io.spine.protodata.Field
import io.spine.protodata.Field.CardinalityCase.SINGLE
import io.spine.protodata.MessageType
import io.spine.protodata.MessageTypeDependencies
import io.spine.protodata.java.ClassName
import io.spine.protodata.type.TypeSystem
import io.spine.tools.mc.java.entity.NestedClassFactory
import io.spine.tools.psi.java.addLast
import org.intellij.lang.annotations.Language

@Suppress("EmptyClass", // ... to avoid false positives for `@Language` strings.
    "UnusedPrivateProperty" // Temporarily until the class is fully implemented.
)
internal class FieldClassFactory(
    type: MessageType,
    typeSystem: TypeSystem
) : NestedClassFactory(type, "Field", typeSystem) {

    private val fieldClass = nestedClass

    // TODO: this should come from settings.
    //  See `GenerateFields.superclass` and usages of `GenerateFields`.
    private val fieldSupertype: ClassName = ClassName(SubscribableField::class.java)

    private val fields: List<Field> by lazy {
        type.fieldList
    }

    @Language("JAVA")
    override fun classJavadoc(): String = """
        /**
         * The listing of $stateJavadocRef fields to be used for creating a subscription filter.
         *
         * <p>Please use static methods of this class to access top-level fields of
         * the entity state type.
         * 
         * <p>Nested fields can be accessed using the values returned by the top-level
         * field accessors, through method chaining.
         */ 
        """.trimIndent()

    override fun tuneClass() {
        addTopLevelFieldMethods()
        addFieldClasses()
    }

    private fun addTopLevelFieldMethods() {
        fields.forEach {
            val accessor = TopLevelFieldAccessor(it, fieldSupertype, typeSystem)
            fieldClass.add(accessor.method())
        }
    }

    private fun addFieldClasses() {
        val nestedFieldTypes =
            MessageTypeDependencies(type, cardinality = SINGLE, typeSystem).set
        nestedFieldTypes.forEach {
            val messageTypeField = MessageTypedField(fieldType = it, fieldSupertype).createClass()
            nestedClass.addLast(messageTypeField)
        }
    }
}
