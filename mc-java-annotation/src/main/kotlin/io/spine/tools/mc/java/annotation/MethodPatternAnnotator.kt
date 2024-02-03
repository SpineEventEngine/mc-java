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

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import io.spine.protodata.codegen.java.codeReference
import io.spine.protodata.codegen.java.file.isJava
import io.spine.protodata.codegen.java.file.psiFile
import io.spine.protodata.codegen.java.isRepeatable
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.tools.mc.java.PsiWriteSupport

internal class MethodPatternAnnotator : PatternAnnotator() {

    override fun loadPatterns(): List<String> =
        settings.internalMethodNameList

    private val annotationCode: String by lazy {
        "@${annotationClass.codeReference}"
    }

    override fun render(sources: SourceFileSet) {
        PsiWriteSupport.execute {
            sources.filter { it.isJava }.forEach {
                annotate(it)
            }
        }
    }

    private fun annotate(file: SourceFile) {
        val javaFile = file.text().psiFile()
        var updated = false
        javaFile.classes.forEach {
            if (annotateInClass(it)) {
                updated = true
            }
        }
        if (updated) {
            val updatedCode = javaFile.text
            file.overwrite(updatedCode)
        }
    }

    private fun annotateInClass(cls: PsiClass): Boolean {
        val annotation by lazy {
            val factory = JavaPsiFacade.getElementFactory(cls.project)
            factory.createAnnotationFromText(annotationCode, cls);
        }
        var updated = false
        cls.methods.filter {
            matches(it.name)
        }.forEach {
            if (annotateMethod(it, annotation)) {
                updated = true
            }
        }
        return updated
    }

    private fun annotateMethod(method: PsiMethod, annotation: PsiAnnotation): Boolean {
        val alreadyAnnotated = method.hasAnnotation(annotationClass.codeReference)
        if (alreadyAnnotated && !annotationClass.isRepeatable) {
            return false
        }
        with(method.modifierList) {
            addBefore(annotation, firstChild)
        }
        return true
    }
}
