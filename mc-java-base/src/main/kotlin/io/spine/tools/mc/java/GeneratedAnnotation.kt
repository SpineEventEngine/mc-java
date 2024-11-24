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
import io.spine.tools.java.reference
import io.spine.tools.mc.java.GeneratedAnnotation.create
import io.spine.tools.mc.java.VersionHolder.version
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.annotation.Generated
import org.intellij.lang.annotations.Language

/**
 * Creates [PsiAnnotation] for marking code elements created by McJava.
 *
 * ## Implementation note
 * We do not cache the created instance of [PsiAnnotation] because PSI elements are mutable.
 * We would like to avoid unwanted propagation of a modification which could be made by
 * one renderer to others.
 *
 * @see Generated
 * @see create
 * @see VersionHolder
 */
public object GeneratedAnnotation {

    /**
     * Creates a new [PsiAnnotation] with [javax.annotation.Generated] referencing the current
     * version of Spine Model Compiler.
     *
     * @param value The string to be put into the annotation `value` parameter.
     *  The default value refers to the current version of Spine Model Compiler.
     */
    public fun create(
        value: String = "by Spine Model Compiler (version: ${version.value})"
    ): PsiAnnotation {
        val reference = Generated::class.java.reference
        @Language("JAVA") @Suppress("EmptyClass")
        val annotation = elementFactory.createAnnotationFromText(
            """
            @$reference($value)
            """.trimIndent(), null
        )
        return annotation
    }
}
