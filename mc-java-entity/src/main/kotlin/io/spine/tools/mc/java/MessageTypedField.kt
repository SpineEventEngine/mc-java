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

package io.spine.tools.mc.java

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaCodeReferenceElement
import io.spine.protodata.MessageType
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.createClassType
import io.spine.protodata.java.javaClassName
import io.spine.protodata.type.TypeSystem
import io.spine.tools.mc.java.entity.field.NestedFieldAccessor
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addFirst
import io.spine.tools.psi.java.addLast
import io.spine.tools.psi.java.createPrivateConstructor
import io.spine.tools.psi.java.makeFinal
import io.spine.tools.psi.java.makePublic
import io.spine.tools.psi.java.makeStatic
import org.intellij.lang.annotations.Language

/**
 * Generates a class which represents a field which has
 * a [Message][com.google.protobuf.Message] type.
 *
 * The generated class extends [SubscribableField][io.spine.base.SubscribableField] or
 * one of its subclasses. As such, it can be passed to message filters and be used to obtain
 * its fields when composing a filter.
 *
 * More formally, for a given [fieldType], this class will generate a Java class which:
 *  1. Is named by combining simple Java class name of the field type and
 *  the ["Field"][MessageTypedField.CLASS_NAME_SUFFIX] suffix.
 *  For example, `UserIdField`.
 *
 *  2. Inherits from [SubscribableField][io.spine.base.SubscribableField] or one of
 *  its descendants such as [EntityStateField][io.spine.query.EntityStateField].
 *
 *  3. Has a `private` constructor which accepts a single parameter of
 *  the [Field][io.spine.base.Field] class. The constructor is `private` because
 *  the generated class is intended to be used only from within the scope of
 *  the outer `Field` class.
 *
 *  4. Exposes nested message fields through the instance methods which append the name of the
 *  requested field to the enclosed field path.
 *
 * The created class is then places under
 * the [Field][io.spine.tools.mc.java.FieldClassFactory.CLASS_NAME] class, which,
 * in turn, is nested under corresponding Java class of the message to which the field belongs.
 *
 * @param fieldType
 *         the type of the field for which we generate the code.
 * @param fieldSupertype
 *         the supertype for the generated class.
 * @param typeSystem
 *         the type system to resolve message types into Java classes.
 */
internal class MessageTypedField(
    private val fieldType: MessageType,
    private val fieldSupertype: ClassName,
    private val typeSystem: TypeSystem
) {
    private val className: String by lazy {
        val typeName = fieldType.name
        val nestingPath = typeName.nestingTypeNameList.joinToString()
        nestingPath + typeName.simpleName + CLASS_NAME_SUFFIX
    }

    private val superClassReference: PsiJavaCodeReferenceElement by lazy {
        val qualifiedName = fieldSupertype.canonical
        elementFactory.createReferenceFromText(qualifiedName, null)
    }

    internal fun createClass(): PsiClass {
        val cls = elementFactory.createClass(className)
        cls.run {
            makePublic().makeStatic().makeFinal()
            addSuperclass()
            addJavadoc()
            addConstructor()
            addFieldMethods()
        }
        return cls
    }

    private fun PsiClass.addJavadoc() {
        val msgClassRef = fieldType.javaClassName(typeSystem).canonical
        @Language("JAVA") @Suppress("EmptyClass")
        val javadoc = elementFactory.createDocCommentFromText("""
            /**
             * Provides fields of the {@link $msgClassRef} message type.
             */                
            """.trimIndent())
        addFirst(javadoc)
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

    private fun PsiClass.addConstructor() {
        val thisClass = this // for references under the `run` block.
        val constructor = elementFactory.run {
            val ctor = createPrivateConstructor(thisClass)
            val fieldClass = createClassType<io.spine.base.Field>()
            val parameter = createParameter("field", fieldClass)
            ctor.parameterList.add(parameter)
            val body = ctor.body!!
            val superCall = createStatementFromText("super(field);", thisClass)
            body.add(superCall)
            ctor
        }
        thisClass.addLast(constructor)
    }

    private fun PsiClass.addFieldMethods() {
        fieldType.fieldList.forEach {
            val accessor = NestedFieldAccessor(it, fieldSupertype, typeSystem)
            add(accessor.method())
        }
    }

    companion object {
        const val CLASS_NAME_SUFFIX = "Field"
    }
}
