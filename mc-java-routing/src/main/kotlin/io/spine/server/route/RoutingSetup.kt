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

package io.spine.server.route

import io.spine.base.CommandMessage
import io.spine.base.EntityState
import io.spine.base.EventMessage
import io.spine.base.MessageContext
import io.spine.core.CommandContext
import io.spine.core.EventContext
import io.spine.server.entity.Entity
import io.spine.type.KnownMessage
import java.util.ServiceLoader

public interface RoutingSetup<
        I : Any,
        M : KnownMessage,
        C : MessageContext,
        R : Any,
        U : MessageRouting<M, C, R>> {

    public fun entityClass(): Class<out Entity<I, *>>
    public fun setup(routing: U)
}

internal typealias RSetup = RoutingSetup<*, *, *, *, *>

internal object RoutingSetupRegistry {

    private val entries: Set<Entry>

    init {
        val setupClasses = setOf(
            CommandRoutingSetup::class,
            EventRoutingSetup::class,
            StateRoutingSetup::class
        )
        val allServices = setupClasses
            .map { it.java }
            .flatMap { ServiceLoader.load(it) }
        val grouped = allServices.groupBy { it.entityClass() }

        entries = grouped.map { (cls, setups) -> Entry(cls, setups) }.toSet()
    }

    fun find(
        entityClass: Class<out Entity<*, *>>,
        setupClass: Class<out RSetup>
    ): RSetup? {
        val entry = entries.find { it.entityClass == entityClass }
        return entry?.find(setupClass)
    }

    private data class Entry(
        val entityClass: Class<out Entity<*, *>>,
        private val setups: List<RSetup>
    ) {
        init {
            // Check the consistency of grouping.
            setups.forEach {
                require(it.entityClass() == entityClass) {
                    val setupClass = it::class.qualifiedName
                    val servedBySetup = it.entityClass().simpleName
                    "The `entityClass` (`${entityClass.simpleName}`) of the entry" +
                            " must match the property of the setup (`$setupClass`)." +
                            " Encountered: `$servedBySetup`."
                }
            }
        }

        fun find(
            setupClass: Class<out RSetup>
        ): RSetup? {
            val found = setups.find { setupClass.isAssignableFrom(it.javaClass) }
            @Suppress("UNCHECKED_CAST") // The cast is protected by the initial check
            return found
        }
    }
}

public interface CommandRoutingSetup<I : Any> :
    RoutingSetup<I, CommandMessage, CommandContext, I, CommandRouting<I>> {

    public companion object {

        public fun <I : Any> apply(cls: Class<out Entity<I, *>>, routing: CommandRouting<I>) {
            val found = RoutingSetupRegistry.find(cls, CommandRoutingSetup::class.java)
            found?.let {
                @Suppress("UNCHECKED_CAST")
                (it as CommandRoutingSetup<I>).setup(routing)
            }
        }
    }
}

public interface EventRoutingSetup<I : Any> :
    RoutingSetup<I, EventMessage, EventContext, Set<I>, EventRouting<I>> {

    public companion object {

        public fun <I : Any> apply(cls: Class<out Entity<I, *>>, routing: EventRouting<I>) {
            val fount = RoutingSetupRegistry.find(cls, EventRoutingSetup::class.java)
            fount?.let {
                @Suppress("UNCHECKED_CAST")
                (it as EventRoutingSetup<I>).setup(routing)
            }
        }
    }
}

public interface StateRoutingSetup<I : Any> :
    RoutingSetup<I, EntityState<*>, EventContext, Set<I>, StateUpdateRouting<I>> {

    public companion object {

        public fun <I : Any> apply(cls: Class<out Entity<I, *>>, routing: StateUpdateRouting<I>) {
            val found = RoutingSetupRegistry.find(cls, StateRoutingSetup::class.java)
            found?.let {
                @Suppress("UNCHECKED_CAST")
                (it as StateRoutingSetup<I>).setup(routing)
            }
        }
    }
}
