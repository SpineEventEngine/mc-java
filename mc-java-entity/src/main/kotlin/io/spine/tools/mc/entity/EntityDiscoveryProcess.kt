/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.tools.mc.entity

import io.spine.core.External
import io.spine.protodata.File
import io.spine.protodata.event.TypeDiscovered
import io.spine.protodata.settings.loadSettings
import io.spine.server.entity.alter
import io.spine.server.event.React
import io.spine.server.procman.ProcessManager
import io.spine.tools.mc.entity.event.EntityStateDiscovered
import io.spine.tools.mc.entity.event.entityStateDiscovered
import io.spine.tools.mc.java.codegen.Entities
import java.util.Optional

/**
 * Gathers entity states declared in proto files.
 */
internal class EntityDiscoveryProcess :
    ProcessManager<File, DiscoveredEntities, DiscoveredEntities.Builder>(),
    EntityPluginComponent {

    /**
     * The settings passed by McJava to [EntityPlugin].
     */
    private val settings: Entities by lazy {
        loadSettings<Entities>()
    }

    /**
     * The names of the message options that make those messages entity types.
     */
    private val options: List<String> by lazy {
        settings.optionList.map { it.name }
    }

    @React
    fun on(@External e: TypeDiscovered): Optional<EntityStateDiscovered> {
        val typeOptions = e.type.optionList.map { it.name }
        if (typeOptions.any { it in options }) {
            alter {
                addType(e.type)
            }
            return Optional.of(entityStateDiscovered {
                name = e.type.name
                file = e.file
                type = e.type
            })
        }
        return Optional.empty()
    }
}