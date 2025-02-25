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

import com.squareup.kotlinpoet.ksp.toClassName

internal class StateUpdateRouteVisitor(
    functions: List<StateUpdateRouteFun>,
    environment: Environment
) : RouteVisitor<StateUpdateRouteFun>(
    environment.stateRoutingSetup,
    functions,
    environment
) {

    override val classNameSuffix: String = "StateUpdateRouting"

    override fun addRoute(fn: StateUpdateRouteFun) {
        val params = if (fn.acceptsContext) "e, c" else "e"
        val entryFn = if (fn.isUnicast) UNICAST_FUN_NAME else ROUTE_FUN_NAME

        routingRunBlock.add(
            "%L<%T> { %L -> %T.%L(%L) }\n",
            entryFn,
            fn.messageClass,
            params,
            entityClass.type.toClassName(),
            fn.decl.simpleName.asString(),
            params
        )
    }

    companion object {

        /**
         * Processes the given route functions using [StateUpdateRouteVisitor].
         */
        fun process(qualified: List<RouteFun>, environment: Environment) {
            runVisitors<StateUpdateRouteVisitor, StateUpdateRouteFun>(
                qualified,
                environment
            ) { functions ->
                StateUpdateRouteVisitor(functions, environment)
            }
        }
    }
}
