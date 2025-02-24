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

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Provides information about a route function detected in the [declaringClass]
 *
 * @property decl The declaration of the function
 * @property declaringClass The class that declares the function.
 * @param parameters The parameter(s) of the function.
 *  If the [Pair.second] property is non-null the function accepts a context parameter.
 * @param returnType The type returned by the function.
 */
internal sealed class RouteFun(
    val decl: KSFunctionDeclaration,
    val declaringClass: EntityClass,
    parameters: Pair<KSType, KSType?>,
    returnType: KSType
) {
    /**
     * The type of the first parameter of the route function.
     */
    val messageParameter: KSType = parameters.first

    /**
     * The class name of the first parameter.
     */
    val messageClass: ClassName = messageParameter.toClassName()

    /**
     * Tells if the function accepts a context parameter.
     */
    val acceptsContext: Boolean = parameters.second != null

    /**
     * Tells if the function returns one identifier rather than a set of identifiers.
     */
    val isUnicast: Boolean = returnType.declaration.typeParameters.isEmpty()
}

/**
 * The declaration of a route function for commands.
 */
internal class CommandRouteFun(
    fn: KSFunctionDeclaration,
    declaringClass: EntityClass,
    parameters: Pair<KSType, KSType?>,
    returnType: KSType
) : RouteFun(fn, declaringClass, parameters, returnType)

/**
 * The declaration of a route function for events.
 */
internal class EventRouteFun(
    fn: KSFunctionDeclaration,
    declaringClass: EntityClass,
    parameters: Pair<KSType, KSType?>,
    returnType: KSType
) : RouteFun(fn, declaringClass, parameters, returnType)

/**
 * The declaration of a route function for entity states.
 */
internal class StateUpdateRouteFun(
    fn: KSFunctionDeclaration,
    declaringClass: EntityClass,
    parameters: Pair<KSType, KSType?>,
    returnType: KSType
) : RouteFun(fn, declaringClass, parameters, returnType)
