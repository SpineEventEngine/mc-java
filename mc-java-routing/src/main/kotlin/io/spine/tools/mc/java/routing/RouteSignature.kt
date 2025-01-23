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
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import io.spine.base.SignalMessage
import io.spine.core.SignalContext
import io.spine.server.route.Route
import io.spine.string.simply
import io.spine.tools.mc.java.routing.RouteSignature.Companion.jvmStaticRef
import io.spine.tools.mc.java.routing.RouteSignature.Companion.routeRef
import msg
import funRef

/**
 * The base class for classes checking the contract of the functions with the [Route] annotation.
 */
internal sealed class RouteSignature<F : RouteFun>(
    protected val signalClass: Class<out SignalMessage>,
    protected val contextClass: Class<out SignalContext>,
    protected val resolver: Resolver,
    protected val logger: KSPLogger
) {
    protected abstract fun parametersMatch(fn: KSFunctionDeclaration): Boolean
    protected abstract fun returnTypeMatches(fn: KSFunctionDeclaration): Boolean
    protected abstract fun declarationSiteMatches(fn: KSFunctionDeclaration): Boolean
    protected abstract fun create(fn: KSFunctionDeclaration): F

    fun match(fn: KSFunctionDeclaration): F? {
        if (!parametersMatch(fn)) {
            return null
        }
        if (!returnTypeMatches(fn)) {
            return null
        }
        if (!declarationSiteMatches(fn)) {
            return null
        }
        return create(fn)
    }

    companion object {

        val routeRef by lazy { "`@${simply<Route>()}`" }
        val jvmStaticRef by lazy { "`@${simply<JvmStatic>()}`" }

        fun qualify(
            functions: Sequence<KSFunctionDeclaration>,
            resolver: Resolver,
            logger: KSPLogger
        ): List<RouteFun> {
            val qualifier = Qualifier(functions, resolver, logger)
            return qualifier.run()
        }
    }
}

/**
 * The helper class which transforms the incoming sequence with [functions] into
 * a list containing [CommandRouteFun] or [EventRouteFun].
 *
 * If a function is not recognized to be one of these types,
 * the compilation terminates with an error.
 */
private class Qualifier(
    private val functions: Sequence<KSFunctionDeclaration>,
    resolver: Resolver,
    private val logger: KSPLogger
) {
    private var errors = false
    private val cmd = CommandRouteSignature(resolver, logger)
    private val evt = EventRouteSignature(resolver, logger)

    fun run(): List<RouteFun> {
        val result = buildList<RouteFun> {
            functions.forEach { fn ->
                if (fn.commonChecks(logger)) {
                    qualify(fn)?.let {
                        add(it)
                    }
                }
            }
        }
        if (errors) {
            error("Errors using $routeRef.")
        }
        return result
    }

    private fun qualify(fn: KSFunctionDeclaration): RouteFun? {
        cmd.match(fn)?.let {
            return it
        } ?: evt.match(fn)?.let {
            return it
        } ?: run {
            logger.error(
                "The ${fn.funRef} does not match the $routeRef contract.", fn
            )
            errors = true
            return null
        }
    }
}

private fun KSFunctionDeclaration.commonChecks(logger: KSPLogger): Boolean =
    declaredInAClass(logger)
            && isStatic(logger)
            && acceptsOneOrTwoParameters(logger)

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
