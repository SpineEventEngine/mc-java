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

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate
import io.spine.server.route.Route

internal class RouteProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private lateinit var environment: Environment

    override fun process(resolver: Resolver): List<KSAnnotated> {
        this.environment = Environment(resolver, logger, codeGenerator)
        val allAnnotated = resolver.getSymbolsWithAnnotation(Route::class.qualifiedName!!)
        val allValid = allAnnotated.filter { it.validate() }
            .map { it as KSFunctionDeclaration }

        val qualified = RouteSignature.qualify(allValid, environment)
        processCommands(qualified)
        processEvents(qualified)
        processStateUpdates(qualified)

        val unprocessed = allAnnotated.filterNot { it.validate() }.toList()
        return unprocessed
    }

    private fun processCommands(qualified: List<RouteFun>) {
        val routing = qualified.filterIsInstance<CommandRouteFun>()
        val grouped = routing.groupByClasses()
        grouped.forEach { (declaringClass, functions) ->
            val crv = CommandRouteVisitor(functions, environment)
            declaringClass.accept(crv, Unit)
            crv.writeFile()
        }
    }

    private fun processEvents(qualified: List<RouteFun>) {
        val routing = qualified.filterIsInstance<EventRouteFun>()
        val grouped = routing.groupByClasses()
        grouped.forEach { (declaringClass, functions) ->
            val erv = EventRouteVisitor(functions, environment)
            declaringClass.accept(erv, Unit)
            erv.writeFile()
        }
    }

    private fun processStateUpdates(qualified: List<RouteFun>) {
        val routing = qualified.filterIsInstance<StateUpdateRouteFun>()
        val grouped = routing.groupByClasses()
        grouped.forEach { (declaringClass, functions) ->
            val erv = StateUpdateRouteVisitor(functions, environment)
            declaringClass.accept(erv, Unit)
            erv.writeFile()
        }
    }
}

private fun <F : RouteFun> List<F>.groupByClasses(): Map<EntityClass, List<F>> =
    groupBy { it.declaringClass }
        .mapValues { (_, list) ->
            RouteFunComparator.sort(list)
        }

/**
 * Compares two [RouteFun] instances by their [messageParameter][RouteFun.messageParameter]
 * properties, putting more abstract type further in the sorting order.
 */
private class RouteFunComparator : Comparator<RouteFun> {

    @Suppress("ReturnCount")
    override fun compare(o1: RouteFun, o2: RouteFun): Int {
        val m1 = o1.messageParameter
        val m2 = o2.messageParameter

        if (m1 == m2) {
            return 0
        }
        // An interface should come after a class in the sorting.
        if (m1.isInterface && !m2.isInterface) {
            return 1
        }
        if (!m1.isInterface && m2.isInterface) {
            return -1
        }
        // Both are either classes or interfaces.
        // The one that is more abstract goes further in sorting.
        if (m1.isAssignableFrom(m2)) {
            return 1
        }
        if (m2.isAssignableFrom(m1)) {
            return -1
        }
        val n1 = m1.declaration.qualifiedName?.asString()
        val n2 = m2.declaration.qualifiedName?.asString()
        return compareValues(n1, n2)
    }

    companion object {

        fun <F : RouteFun> sort(list: List<F>): List<F> {
            val comparator = RouteFunComparator()
            return list.sortedWith(comparator)
        }
    }
}

