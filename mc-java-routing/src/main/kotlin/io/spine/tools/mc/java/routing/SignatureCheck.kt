/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.routing

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import io.spine.tools.mc.java.routing.SignatureCheck.Companion.annotationRef

/**
 * Verifies that all [functions] of the [declaringClass] satisfy the contract
 * of the [@Route][io.spine.server.route.Route] annotation.
 */
internal class SignatureCheck(
    @Suppress("unused") private val declaringClass: KSClassDeclaration,
    private val functions: List<KSFunctionDeclaration>,
    private val logger: KSPLogger
) {

    fun apply() {
        functions.forEach { it.checkUsage(logger) }
    }

    @Suppress("ConstPropertyName") // https://bit.ly/kotlin-prop-names
    internal companion object {

        const val annotationRef = "`@Route`"
    }
}

private fun KSFunctionDeclaration.checkUsage(logger: KSPLogger) {
    if (functionKind != FunctionKind.STATIC) {
        val methodName = simpleName.getShortName()
        logger.error(
            "The method `$methodName()` annotated with $annotationRef must be `static`.",
            this
        )
    }
}
