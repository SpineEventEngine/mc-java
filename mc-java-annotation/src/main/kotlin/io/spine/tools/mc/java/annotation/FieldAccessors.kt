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

package io.spine.tools.mc.java.annotation

import com.intellij.psi.PsiMethod
import io.spine.protodata.FieldName
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.file.locate
import io.spine.protodata.renderer.InsertionPoint
import io.spine.string.Separator
import io.spine.string.camelCase
import io.spine.text.Text
import io.spine.text.TextCoordinates
import io.spine.tools.psi.document

/**
 * An insertion point for the methods generated for a message field.
 *
 * The insertion point matches all the methods that contain a `CamelCase` version
 * of the field name in their name. As such it would match methods in a message class,
 * in a builder class, and in a `MessageOrBuilder` interface.
 */
internal class FieldAccessors(
    private val className: ClassName,
    private val field: FieldName
) : InsertionPoint {

    override val label: String
        get() = ""

    override fun locate(text: Text): Set<TextCoordinates> = buildSet {
        val psiClass = text.locate(className)
        check(psiClass != null) {
            "Unable to find the class `${className.canonical}` in the code below:" +
                    Separator.nl().repeat(2) + text.value
        }
        val camelCase = field.value.camelCase()
        psiClass.methods.forEach { method ->
            if (method.name.contains(camelCase)) {
                val lineNumber = method.modifierListStart()
                add(atLine(lineNumber))
            }
        }
        if (this.isEmpty()) {
            val qualifiedFieldName = className.canonical + "." + field.value
            val errorMsg = "Unable to find getter(s) for the field `$qualifiedFieldName`" +
                    " in the code below:" + Separator.nl().repeat(2) + text.value
            error(errorMsg)
        }
    }
}

/**
 * Returns the line number of the first non-Javadoc part of the method.
 */
private fun PsiMethod.modifierListStart(): Int {
    val offset = modifierList.textRange.startOffset
    val lineNumber = containingClass!!.document.getLineNumber(offset)
    return lineNumber
}
