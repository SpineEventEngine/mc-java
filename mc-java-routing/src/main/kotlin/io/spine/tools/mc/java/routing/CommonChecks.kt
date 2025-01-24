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
import funRef
import io.spine.server.entity.Entity
import io.spine.tools.mc.java.routing.RouteSignature.Companion.jvmStaticRef
import io.spine.tools.mc.java.routing.RouteSignature.Companion.routeRef
import msg

/**
 * Runs general usage checks for this function declaration.
 *
 * @param logger The logger to report errors or warnings, if any.
 * @return `true` if all the checks pass, `false` otherwise.
 */
internal fun KSFunctionDeclaration.commonChecks(context: Context): Boolean {
    val logger = context.logger
    // Run all the checks assuming that compilation may terminate after more than one error.
    val declaredInAClass = declaredInAClass(logger)
    val isStatic = isStatic(logger)
    val acceptsOneOrTwoParameters = acceptsOneOrTwoParameters(logger)
    return (declaredInAClass
            && isStatic
            && acceptsOneOrTwoParameters)
}

private fun KSFunctionDeclaration.isStatic(logger: KSPLogger): Boolean {
    val isStatic = functionKind == FunctionKind.STATIC
    if (!isStatic) {
        logger.error(msg(
            "The $funRef annotated with $routeRef must be" +
                    " a member of a companion object and annotated with $jvmStaticRef.",

            "The $funRef annotated with $routeRef must be `static`."
        ),
            this
        )
    }
    return isStatic
}

private fun KSFunctionDeclaration.declaredInAClass(logger: KSPLogger): Boolean {
    val inClass = parentDeclaration is KSClassDeclaration
    if (!inClass) {
        // This case is Kotlin-only because in Java a function would belong to a class.
        logger.error(
            "The $funRef annotated with $routeRef must be" +
                    " a member of a companion object of an entity class" +
                    " annotated with $jvmStaticRef.",
            this
        )
    }
    return inClass
}

private fun KSFunctionDeclaration.acceptsOneOrTwoParameters(logger: KSPLogger): Boolean {
    val wrongNumber = parameters.isEmpty() || parameters.size > 2
    if (wrongNumber) {
        logger.error(
            "The $funRef annotated with $routeRef must accept one or two parameters. " +
                    "Encountered: ${parameters.size}.",
            this
        )
    }
    return !wrongNumber
}

internal fun KSFunctionDeclaration.declaringClass(context: Context): EntityClass? {
    val parent = parentDeclaration!!.qualifiedName!!
    var declaringClass = context.resolver.getClassDeclarationByName(parent)!!
    if (declaringClass.isCompanionObject) {
        // In Kotlin routing functions are declared in a companion object.
        // We need the enclosing entity class.
        declaringClass = declaringClass.parentDeclaration!! as KSClassDeclaration
    }
    if (!context.entityInterface.isAssignableFrom(declaringClass.asStarProjectedType())) {
        context.logger.error(
            "The declaring class of the ${funRef} annotated with $routeRef" +
                    " must implement the `${Entity::class.java.canonicalName}` interface.",
            this
        )
        return null
    }
    return EntityClass(declaringClass, context.entityInterface)
}
