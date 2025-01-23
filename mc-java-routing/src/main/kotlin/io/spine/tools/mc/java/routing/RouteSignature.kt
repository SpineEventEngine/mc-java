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
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import funRef
import io.spine.base.SignalMessage
import io.spine.core.SignalContext
import io.spine.server.route.Route
import io.spine.string.simply
import io.spine.tools.mc.java.routing.RouteSignature.Companion.routeRef

/**
 * The base class for classes checking the contract of the functions with the [Route] annotation.
 */
internal sealed class RouteSignature<F : RouteFun>(
    protected val signalClass: Class<out SignalMessage>,
    protected val contextClass: Class<out SignalContext>,
    protected val resolver: Resolver,
    protected val logger: KSPLogger
) {
    private val signalType by lazy { signalClass.toType(resolver) }
    private val contextType by lazy { contextClass.toType(resolver) }

    protected abstract fun returnTypeMatches(fn: KSFunctionDeclaration): Boolean
    protected abstract fun declarationSiteMatches(fn: KSFunctionDeclaration): Boolean
    protected abstract fun create(fn: KSFunctionDeclaration): F

    @Suppress("ReturnCount")
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

    @OverridingMethodsMustInvokeSuper
    protected open fun parametersMatch(fn: KSFunctionDeclaration): Boolean  {
        checkParamSize(fn)

        val firstParamType = fn.parameters[0].type.resolve()
        if (!signalType.isAssignableFrom(firstParamType)) {
            // Even if the parameter does not match, it could be another kind of
            // routing function, so we simply return `false`.
            return false
        }
        if (fn.parameters.size == 2) {
            val secondParamType = fn.parameters[1].type.resolve()
            val match = contextType.isAssignableFrom(secondParamType)
            if (!match) {
                // Here, knowing that the first parameter type is correct, we can complain
                // about the type of the second parameter.
                val actualSecondParamName = secondParamType.declaration.simpleName.getShortName()
                logger.error(
                    "The second parameter of the ${fn.funRef} annotated with $routeRef" +
                            " must be `${contextClass.simpleName}`." +
                            " Encountered: `$actualSecondParamName`.",
                    fn
                )
            }
            return match
        }
        return true
    }

    /**
     * A safety net to accept functions with the proper number of parameters.
     *
     * We formally check for this to be true in [KSFunctionDeclaration.acceptsOneOrTwoParameters].
     */
    private fun checkParamSize(fn: KSFunctionDeclaration) =
        require(fn.parameters.size == 1 || fn.parameters.size == 2)

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
 * Converts this class into [KSType] using the given resolver.
 */
private fun Class<*>.toType(resolver: Resolver): KSType {
    val name = resolver.getKSNameFromString(canonicalName)
    val classDecl = resolver.getClassDeclarationByName(name)
    // This is a reminder to add corresponding JAR to `KotlinCompilation` in tests.
    check(classDecl != null) {
        "Unable to find the declaration of `$canonicalName`." +
                " Make sure the class is in the compilation classpath."
    }
    val type = classDecl.asStarProjectedType()
    return type
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
    private var errorCount = 0
    private val cmd = CommandRouteSignature(resolver, logger)
    private val evt = EventRouteSignature(resolver, logger)

    fun run(): List<RouteFun> {
        val result = mutableListOf<RouteFun>()
        functions.forEach { fn ->
            val commonChecksPass = fn.commonChecks(logger)
            if (!commonChecksPass) {
                errorCount += 1
                return@forEach
            }
            val qualified = qualify(fn)
            if (qualified != null) {
                result.add(qualified)
            } else {
                errorCount += 1
            }
        }
        if (errorCount > 0) {
            error("${"Error".pluralize(errorCount)} using $routeRef.")
        }
        return result
    }

    private fun qualify(fn: KSFunctionDeclaration): RouteFun? {
        cmd.match(fn)?.let {
            return it
        } ?: evt.match(fn)?.let {
            return it
        } ?: return null
    }
}

private fun String.pluralize(count: Int, pluralForm: String? = null): String {
    return if (count == 1) this else pluralForm ?: "${this}s"
}
