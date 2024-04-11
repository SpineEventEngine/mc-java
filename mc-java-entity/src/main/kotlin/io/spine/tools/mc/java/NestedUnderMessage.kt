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
import io.spine.protodata.MessageType
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.file.toPsi
import io.spine.protodata.java.javaClassName
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.type.TypeSystem
import io.spine.tools.code.manifest.Version
import io.spine.tools.mc.java.entity.column.ColumnClassFactory.Companion.logger
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addFirst
import io.spine.tools.psi.java.addLast
import io.spine.tools.psi.java.createPrivateConstructor
import io.spine.tools.psi.java.makeFinal
import io.spine.tools.psi.java.makePublic
import io.spine.tools.psi.java.makeStatic
import io.spine.tools.psi.java.topLevelClass
import org.intellij.lang.annotations.Language

/**
 * Abstract base for classes rendering classes nested into message types.
 *
 * @param type
 *         the type of the message.
 * @param className
 *         a simple name of the nested class to be generated.
 * @param typeSystem
 *         the type system used for resolving field types.
 */
@Suppress("EmptyClass") // ... to avoid false positives for `@Language` strings.
internal abstract class NestedUnderMessage(
    protected val type: MessageType,
    protected val className: String,
    protected val typeSystem: TypeSystem
) {
    /**
     * The product of the factory.
     */
    protected val cls by lazy {
        createClass()
    }

    private fun createClass(): PsiClass {
        val cls = elementFactory.createClass(className)
        cls.commonSetup()
        return cls
    }

    /**
     * The name of the entity state class under which the [cls] is going to places.
     */
    protected val messageClass: ClassName by lazy {
        type.javaClassName(typeSystem)
    }

    /**
     * Reference to [messageClass] made in Javadoc.
     */
    protected val messageJavadocRef: String = "{@link ${messageClass.simpleName}}"

    /**
     * A callback to tune the [cls] in addition to the actions performed during
     * the lazy initialization of the property.
     */
    abstract fun tuneClass()

    /**
     * A callback for creating a Javadoc comment of the class produced by this factory.
     *
     * Implementing methods may use [messageJavadocRef] to reference the class for which
     * this factory produces a [cls].
     */
    abstract fun classJavadoc(): String

    /**
     * Adds a nested class the top class of the given [file].
     *
     * @param file
     *         the Java file to add the class produced by this factory.
     */
    @Suppress("TooGenericExceptionCaught") // ... to log diagnostic.
    fun render(file: SourceFile) {
        try {
            tuneClass()
            val psiJavaFile = file.toPsi()
            val topLevelClass = psiJavaFile.topLevelClass
            topLevelClass.addLast(cls)

            val updatedText = psiJavaFile.text
            file.overwrite(updatedText)
        } catch (e: Throwable) {
            logger.atError().withCause(e).log { """
                Caught exception while generating the `$className` class in `$messageClass`.
                Message: `${e.message}`.                                
                """.trimIndent()
            }
            throw e
        }
    }

    private fun PsiClass.commonSetup() {
        makePublic().makeStatic().makeFinal()
        val ctor = elementFactory.createPrivateConstructor(
            this,
            javadocLine = "Prevents instantiation of this class."
        )
        addLast(ctor)
        addAnnotation()
        addClassJavadoc()
    }

    private fun PsiClass.addAnnotation() {
        val version = Version.fromManifestOf(this::class.java).value
        @Language("JAVA")
        val annotation = elementFactory.createAnnotationFromText(
            """
            @javax.annotation.Generated("by Spine Model Compiler (version: $version)")
            """.trimIndent(), null
        )
        addFirst(annotation)
    }

    private fun PsiClass.addClassJavadoc() {
        val classJavadoc = elementFactory.createDocCommentFromText(classJavadoc(), null)
        addFirst(classJavadoc)
    }
}
