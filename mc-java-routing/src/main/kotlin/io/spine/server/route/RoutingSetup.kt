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

public interface RoutingSetup<
        I : Any,
        M : KnownMessage,
        C : MessageContext,
        R : Any,
        U : MessageRouting<M, C, R>> {

    public fun setup(routing: U)
}

public sealed class RoutingSetupDiscovery<
        M : KnownMessage,
        C : MessageContext,
        U : MessageRouting<M, C, *>
        >(public val classSuffix: String) {

    public fun <I : Any> serving(cls: Class<out Entity<I, *>>): RoutingSetup<I, M, C, *, U>? {
        val setupClassName = cls.name + classSuffix
        try {
            val setupClass = Class.forName(setupClassName).kotlin
            @Suppress("UNCHECKED_CAST")
            return setupClass.objectInstance as RoutingSetup<I, M, C, *, U>
        } catch (_: ClassNotFoundException) {
            // No generated class found.
            return null
        }
    }
}

public interface CommandRoutingSetup<I : Any> :
    RoutingSetup<I, CommandMessage, CommandContext, I, CommandRouting<I>> {

    public companion object :
        RoutingSetupDiscovery<CommandMessage, CommandContext, CommandRouting<Any>>(
            "CommandRouting"
        ) {

        public fun <I : Any> apply(cls: Class<out Entity<I, *>>, routing: CommandRouting<I>) {
            @Suppress("UNCHECKED_CAST")
            val discovered = serving(cls) as CommandRoutingSetup<I>?
            discovered?.setup(routing)
        }
    }
}

public interface EventRoutingSetup<I : Any> :
    RoutingSetup<I, EventMessage, EventContext, Set<I>, EventRouting<I>> {

    public companion object :
        RoutingSetupDiscovery<EventMessage, EventContext, EventRouting<Any>>(
            "EventRouting"
        ) {
        public fun <I : Any> apply(cls: Class<out Entity<I, *>>, routing: EventRouting<I>) {
            @Suppress("UNCHECKED_CAST")
            val discovered = serving(cls) as EventRoutingSetup<I>?
            discovered?.setup(routing)
        }
    }
}

public interface StateRoutingSetup<I : Any> :
    RoutingSetup<I, EntityState<*>, EventContext, Set<I>, StateUpdateRouting<I>> {

    public companion object :
        RoutingSetupDiscovery<EntityState<*>, EventContext, StateUpdateRouting<*>>(
            "StateUpdateRouting"
        ) {

        public fun <I : Any> apply(cls: Class<out Entity<I, *>>, routing: StateUpdateRouting<I>) {
            @Suppress("UNCHECKED_CAST")
            val discovered = serving(cls) as StateRoutingSetup<I>?
            discovered?.setup(routing)
        }
    }
}
