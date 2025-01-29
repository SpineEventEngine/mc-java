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

package io.spine.given.home

import com.google.auto.service.AutoService
import io.spine.core.Subscribe
import io.spine.given.home.events.DeviceMoved
import io.spine.given.home.events.RoomAdded
import io.spine.given.home.events.RoomEvent
import io.spine.given.home.events.RoomRenamed
import io.spine.protobuf.isDefault
import io.spine.server.BoundedContext
import io.spine.server.entity.Entity
import io.spine.server.entity.alter
import io.spine.server.projection.Projection
import io.spine.server.projection.ProjectionRepository
import io.spine.server.route.EventRouting
import io.spine.server.route.EventRoutingSetup
import io.spine.server.route.Route
import io.spine.server.route.StateRoutingSetup
import io.spine.server.route.StateUpdateRouting

fun homeAutomation(): BoundedContext = BoundedContext.singleTenant("HomeAutomation")
    .add(RoomProjectionRepository())
    .build()

public class RoomProjection : Projection<RoomId, Room, Room.Builder>() {

    @Subscribe
    fun on(e: RoomAdded) = alter {
        name = e.name
    }

    @Subscribe
    fun on(e: RoomRenamed) = alter {
        name = e.name
    }

    @Subscribe
    fun on(e: DeviceMoved) = alter {
        if (id == e.prevRoom) {
            val toRemove = deviceBuilderList.find { b -> b.uuid == e.device.uuid }
            if (toRemove != null) {
                deviceBuilderList.remove(toRemove)
            }
        }
        if (id == e.room) {
            addDevice(e.device)
        }
    }

    companion object {

        @Route
        @JvmStatic
        fun route(e: RoomEvent): RoomId = e.room

        @Route
        @JvmStatic
        fun routeMoved(e: DeviceMoved): Set<RoomId> =
            if (e.prevRoom.isDefault()) setOf(e.room) else setOf(e.prevRoom, e.room)
    }
}

internal class RoomProjectionRepository : ProjectionRepository<RoomId, RoomProjection, Room>() {

    override fun setupEventRouting(routing: EventRouting<RoomId>) {
        super.setupEventRouting(routing)

        // Remove routs added via reflective class analysis.
        routing.run {
            remove<RoomEvent>()
            remove<DeviceMoved>()
        }

        EventRoutingSetup.apply(entityClass(), routing)
    }

    override fun setupStateRouting(routing: StateUpdateRouting<RoomId>) {
        super.setupStateRouting(routing)
        StateRoutingSetup.apply(entityClass(), routing)
    }
}

/**
 * This class simulates the generated code.
 */
@AutoService(EventRoutingSetup::class)
public class RoomProjectionEventRoutingX : EventRoutingSetup<RoomId> {

    override fun entityClass(): Class<out Entity<RoomId, *>> = RoomProjection::class.java

    override fun setup(routing: EventRouting<RoomId>) {
        routing.run {
            route<DeviceMoved> { e, _ -> RoomProjection.routeMoved(e) }
            unicast<RoomEvent> { e -> RoomProjection.route(e) }
        }
    }
}
