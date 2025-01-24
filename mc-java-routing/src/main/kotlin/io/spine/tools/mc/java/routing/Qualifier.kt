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

/**
 * The helper class which transforms the incoming sequence with [functions] into
 * a list containing [CommandRouteFun] or [EventRouteFun].
 *
 * If a function is not recognized to be one of these types,
 * the compilation terminates with an error.
 */
internal class Qualifier(
    private val functions: Sequence<KSFunctionDeclaration>,
    private val context: Context
) {
    private var errorCount = 0
    private val cmd = CommandRouteSignature(context)
    private val evt = EventRouteSignature(context)

    fun run(): List<RouteFun> {
        val result = mutableListOf<RouteFun>()
        functions.forEach { fn ->
            val commonChecksPass = fn.commonChecks(context)
            if (!commonChecksPass) {
                errorCount += 1
                return@forEach
            }
            val declaringClass = fn.declaringClass(context)
            if (declaringClass == null) {
                errorCount += 1
                return@forEach
            }
            val qualified = qualify(fn, declaringClass)
            if (qualified != null) {
                result.add(qualified)
            } else {
                errorCount += 1
            }
        }
        if (errorCount > 0) {
            error("${"Error".pluralize(errorCount)} using ${RouteSignature.Companion.routeRef}.")
        }
        return result
    }

    private fun qualify(fn: KSFunctionDeclaration, declaringClass: EntityClass): RouteFun? {
        cmd.match(fn, declaringClass)?.let {
            return it
        } ?: evt.match(fn, declaringClass)?.let {
            return it
        } ?: return null
    }
}
