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

package io.spine.tools.mc.java

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import io.spine.logging.WithLogging
import io.spine.protodata.MessageType
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.javaClassName
import io.spine.protodata.renderer.RenderAction
import io.spine.protodata.renderer.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addFirst
import io.spine.tools.psi.java.addLast
import io.spine.tools.psi.java.createPrivateConstructor
import io.spine.tools.psi.java.makeFinal
import io.spine.tools.psi.java.makePublic
import io.spine.tools.psi.java.makeStatic
import io.spine.tools.psi.java.topLevelClass

/**
 * Abstract base for code generators creating classes nested into Java code of message types.
 *
 * @param type
 *         the type of the message.
 * @param className
 *         a simple name of the nested class to be generated.
 */
public abstract class NestedClassAction(
    protected val type: MessageType,
    protected val className: String,
) : RenderAction<Java, MessageType>(Java, type), WithLogging {

    /**
     * The product of the code generator.
     */
    protected val cls: PsiClass by lazy {
        createClass()
    }

    private fun createClass(): PsiClass {
        val c = elementFactory.createClass(className)
        c.commonSetup()
        return c
    }

    /**
     * The name of the message class under which the [cls] is going to places.
     */
    protected val messageClass: ClassName by lazy {
        type.javaClassName(typeSystem!!)
    }

    /**
     * Reference to [messageClass] which can be made in Javadoc.
     *
     * The reference is a link to the simple class name of the enclosing class.
     */
    protected val messageJavadocRef: String = "{@link ${messageClass.simpleName}}"

    /**
     * A callback to tune the [cls] in addition to the actions performed during
     * the lazy initialization of the property.
     */
    protected abstract fun tuneClass()

    /**
     * A callback for creating a Javadoc comment of the class produced by this factory.
     *
     * Implementing methods may use [messageJavadocRef] to reference the class for which
     * this factory produces a nested class [cls].
     */
    protected abstract fun classJavadoc(): String

    /**
     * Creates the constructor for the class.
     *
     * Default implementation creates a parameterless private constructor.
     */
    protected open fun createConstructor(cls: PsiClass): PsiMethod {
        val ctor = elementFactory.createPrivateConstructor(
            cls,
            javadocLine = "Prevents instantiation of this class."
        )
        return ctor
    }

    /**
     * Adds a nested class the top class of the given [file].
     *
     * @param file
     *         the Java file to add the class produced by this factory.
     */
    @Suppress("TooGenericExceptionCaught") // ... to log diagnostic.
    override fun render(file: SourceFile<Java>) {
        try {
            tuneClass()
            val psiJavaFile = file.psi() as PsiJavaFile
            val targetClass = psiJavaFile.findClass(messageClass)
            targetClass.addLast(cls)

            val updatedText = psiJavaFile.text
            file.overwrite(updatedText)
        } catch (e: Throwable) {
            logger.atError().withCause(e).log { """
                Caught exception while generating the `$className` class in `$messageClass`.
                Message: ${e.message}.                                
                """.trimIndent()
            }
            throw e
        }
    }

    private fun PsiClass.commonSetup() {
        makePublic().makeStatic().makeFinal()
        val ctor = createConstructor(this)
        addLast(ctor)
        addAnnotation()
        addClassJavadoc()
    }

    /**
     * Generates an annotation to be added for the created class.
     *
     * The default implementation returns the result of [GeneratedAnnotation.create].
     *
     * Overriding methods may return custom annotation or `null`, if no annotation is needed.
     */
    protected open fun createAnnotation(): PsiAnnotation? = GeneratedAnnotation.create()

    private fun PsiClass.addAnnotation() {
        val annotation = createAnnotation()
        annotation?.let {
            addFirst(it)
        }
    }

    private fun PsiClass.addClassJavadoc() {
        val text = classJavadoc()
        val classJavadoc = elementFactory.createDocCommentFromText(text, null)
        addFirst(classJavadoc)
    }
}

/**
 * Locates the class with the given name in this [PsiJavaFile].
 *
 * If the given class is nested, the function finds the class nested into the top level class
 * of this Java file. Otherwise, top level class is returned.
 *
 * This is a naïve implementation of locating a class in a Java file that serves our needs for
 * handling top level message classes of entity states, command messages, and events.
 * For rejection messages, we use the logic of one level nesting.
 *
 * If more levels are needed or there is a change of mismatching a class name with a file, this
 * extension function should be rewritten with due test coverage.
 */
private fun PsiJavaFile.findClass(cls: ClassName): PsiClass {
    val targetClass =
        if (cls.isNested)
            topLevelClass.findInnerClassByName(cls.simpleName, false)
        else
            topLevelClass
    check(targetClass != null) {
        "Unable to locate the `${cls.canonical}` class."
    }
    return targetClass
}
