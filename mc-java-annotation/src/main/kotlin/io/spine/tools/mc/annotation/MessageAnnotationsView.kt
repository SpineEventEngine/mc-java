/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.tools.mc.annotation

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.protodata.TypeName
import io.spine.protodata.event.FieldOptionDiscovered
import io.spine.protodata.event.TypeOptionDiscovered
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.server.entity.alter
import io.spine.server.route.EventRouting
import io.spine.tools.mc.annotation.event.FileOptionMatched

/**
 * Gathers the options defined for a message type.
 *
 * Subscribes to [TypeOptionDiscovered] for obtaining directly set options.
 *
 * Subscribes to [FileOptionMatched] events for getting matches between file level options,
 * and type options that are assumed for all the types in the file.
 */
internal class MessageAnnotationsView :
    View<TypeName, MessageAnnotations, MessageAnnotations.Builder>() {

    @Subscribe
    fun on(@External e: TypeOptionDiscovered) = alter {
        // If the option was defined at the file level, overwrite it.
        optionBuilderList.find { it.name == e.option.name }?.let {
            it.value = e.option.value
            return@alter
        }
        addOption(e.option)
    }

    @Subscribe
    fun on(e: FileOptionMatched) = alter {
        // If the option is already present at the message level, do not overwrite it.
        optionList.find { it.name == e.assumed.name }?.let {
            return@alter
        }
        addOption(e.assumed)
    }

    @Subscribe
    fun on(@External e: FieldOptionDiscovered) = alter {
        val fieldOptions = fieldOptionsBuilderList.find { it.field == e.field }
        fieldOptions?.let {
            it.addOption(e.option)
            return@alter
        }
        addFieldOptions(
            fieldOptions {
                field = e.field
                option.add(e.option)
            }
        )
    }

    /**
     * The repository for [MessageAnnotationsView] which tunes the routing of events.
     */
    class Repository: ViewRepository<TypeName, MessageAnnotationsView, MessageAnnotations>() {

        override fun setupEventRouting(routing: EventRouting<TypeName>) {
            super.setupEventRouting(routing)
            routing.route<FileOptionMatched> { e, _ ->
                e.toMessageTypeName()
            }.unicast<TypeOptionDiscovered> { e, _ ->
                e.type
            }.unicast<FieldOptionDiscovered> { e, _ ->
                e.type
            }
        }
    }
}
