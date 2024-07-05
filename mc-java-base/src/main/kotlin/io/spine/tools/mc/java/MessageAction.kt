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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import io.spine.logging.WithLogging
import io.spine.protodata.CodegenContext
import io.spine.protodata.MessageType
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.javaClassName
import io.spine.protodata.renderer.MessageAction
import io.spine.protodata.renderer.SourceFile
import io.spine.tools.code.Java

/**
 * Abstract base for code generation actions for message types in Java code.
 *
 * @property type the type of the message.
 * @property file the source code to which the action is applied.
 * @property context the code generation context in which this action runs.
 */
public abstract class MessageAction(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : MessageAction<Java>(Java, type, file, context), WithLogging {

    /**
     * The [file] parsed into instance of [PsiJavaFile].
     */
    protected val psiFile: PsiJavaFile by lazy {
        file.psi() as PsiJavaFile
    }

    /**
     * The name of the message class under which [cls] is going to places.
     */
    protected val messageClass: ClassName by lazy {
        type.javaClassName(typeSystem!!)
    }

    /**
     * The target of the code generation action.
     */
    protected abstract val cls: PsiClass

    /**
     * Modifies the class referenced by the [cls] property.
     *
     * The update of the source code is performed by the [render] method which calls
     * [doRender] for doing the job of the action.
     */
    protected abstract fun doRender()

    /**
     * Reference to [messageClass] which can be made in Javadoc.
     *
     * The reference is a link to the simple class name of the enclosing class.
     */
    protected val messageJavadocRef: String = "{@link ${messageClass.simpleName}}"

    /**
     * Adds a nested class the top class of the given [file].
     */
    @Suppress("TooGenericExceptionCaught") // ... to log diagnostic.
    override fun render() {
        try {
            doRender()
            val updatedText = psiFile.text
            file.overwrite(updatedText)
        } catch (e: Throwable) {
            logger.atError().withCause(e).log { """
                Caught exception while applying `$this`.
                Message: ${e.message}.                                
                """.trimIndent()
            }
            throw e
        }
    }

    override fun toString(): String {
        return "MessageAction(type=$type, file=$file)"
    }
}
