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

package io.spine.tools.mc.java.routing.proessor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Origin.JAVA
import com.google.devtools.ksp.symbol.Origin.KOTLIN
import funRef
import io.spine.server.entity.Entity
import io.spine.tools.mc.java.routing.proessor.RouteSignature.Companion.jvmStaticRef
import io.spine.tools.mc.java.routing.proessor.RouteSignature.Companion.routeRef
import msg

/**
 * Runs general usage checks for this function declaration.
 *
 * The function runs all the checks, assuming that compilation does not terminate after
 * an error is reported via [KSPLogger.error].
 *
 * @param environment The environment for resolving types and reporting errors or warnings.
 * @return The number of detected errors, or zero if no errors were found.
 */
internal fun KSFunctionDeclaration.commonChecks(environment: Environment): Int {
    val logger = environment.logger
    val declaredInAClass = declaredInAClass(logger)
    val isStatic = isStatic(logger)
    val acceptsOneOrTwoParameters = acceptsOneOrTwoParameters(logger)
    return (declaredInAClass
            + isStatic
            + acceptsOneOrTwoParameters)
}

private fun Boolean.toErrorCount(): Int = if (this) 0 else 1

private fun KSFunctionDeclaration.isStatic(logger: KSPLogger): Int {
    val isStatic = when (origin) {
        JAVA -> functionKind == FunctionKind.STATIC
        KOTLIN -> parentDeclaration is KSClassDeclaration &&
                (parentDeclaration as KSClassDeclaration).isCompanionObject
        else -> false
    } 
    if (!isStatic) {
        logger.error(msg(
            "The $funRef annotated with $routeRef must be a member of a companion object.",
            "The $funRef annotated with $routeRef must be `static`."
        ),
            this
        )
    }
    return isStatic.toErrorCount()
}

private fun KSFunctionDeclaration.declaredInAClass(logger: KSPLogger): Int {
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
    return inClass.toErrorCount()
}

private fun KSFunctionDeclaration.acceptsOneOrTwoParameters(logger: KSPLogger): Int {
    val wrongNumber = parameters.isEmpty() || parameters.size > 2
    if (wrongNumber) {
        logger.error(
            "The $funRef annotated with $routeRef must accept one or two parameters. " +
                    "Encountered: ${parameters.size}.",
            this
        )
    }
    return (!wrongNumber).toErrorCount()
}

internal fun KSFunctionDeclaration.declaringClass(environment: Environment): EntityClass? {
    val parent = parentDeclaration!!.qualifiedName!!
    var declaringClass = environment.resolver.getClassDeclarationByName(parent)!!
    if (declaringClass.isCompanionObject) {
        // In Kotlin routing functions are declared in a companion object.
        // We need the enclosing entity class.
        declaringClass = declaringClass.parentDeclaration!! as KSClassDeclaration
    }
    if (!environment.entityInterface.isAssignableFrom(declaringClass.asStarProjectedType())) {
        environment.logger.error(
            "The declaring class of the $funRef annotated with $routeRef" +
                    " must implement the `${Entity::class.java.canonicalName}` interface.",
            this
        )
        return null
    }
    return EntityClass(declaringClass, environment.entityInterface)
}
