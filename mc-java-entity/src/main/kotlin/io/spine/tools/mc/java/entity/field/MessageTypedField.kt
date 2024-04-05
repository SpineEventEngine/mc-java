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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaCodeReferenceElement
import io.spine.protodata.MessageType
import io.spine.protodata.java.ClassName
import io.spine.tools.psi.java.Environment.elementFactory

/**
 * Generates a class which represents a field which has
 * a [Message][com.google.protobuf.Message] type.
 *
 * The generated class extends [SubscribableField][io.spine.base.SubscribableField] or
 * one of its subclasses. As such, it can be passed to message filters and be used to obtain
 * its fields when composing a filter.
 *
 * More formally, for a given [fieldType], this class will generate a Java class which:
 *  1. Is named by combining simple Java class name of the field type and `Field` suffix.
 *     For example, `UserIdField`.
 *  2. Inherits from [SubscribableField][io.spine.base.SubscribableField] or one of
 *     its descendants such as [EntityStateField][io.spine.query.EntityStateField].
 *  3. Exposes nested message fields through the instance methods which append the name of the
 *     requested field to the enclosed field path.
 *
 * The created class then places under the `Field` class nested under corresponding
 * [EntityState][io.spine.base.EntityState] class.
 */
internal class MessageTypedField(
    private val fieldType: MessageType,
    private val fieldSupertype: ClassName
) {

    private val className: String by lazy {
        fieldType.name.simpleName + "Field"
    }

    private val superClassReference: PsiJavaCodeReferenceElement by lazy {
        val qualifiedName = fieldSupertype.canonical
        elementFactory.createReferenceFromText(qualifiedName, null)
    }

    internal fun createClass(): PsiClass {
        val cls = elementFactory.createClass(className)
        cls.addSuperclass()
        return cls
    }

    private fun PsiClass.addSuperclass() {
        val superTypes = extendsList
        if (superTypes == null) {
            val newList = elementFactory.createReferenceList(arrayOf(
                superClassReference
            ))
            addBefore(newList, lBrace)
        } else {
            superTypes.add(superClassReference)
        }
    }
}

