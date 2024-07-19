/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.uuid

import io.spine.core.External
import io.spine.protodata.MessageType
import io.spine.protodata.PrimitiveType
import io.spine.protodata.event.TypeDiscovered
import io.spine.protodata.plugin.Policy
import io.spine.protodata.settings.loadSettings
import io.spine.server.event.React
import io.spine.server.model.NoReaction
import io.spine.server.tuple.EitherOf2
import io.spine.tools.mc.java.settings.Uuids
import io.spine.tools.mc.java.uuid.event.UuidValueDiscovered
import io.spine.tools.mc.java.uuid.event.uuidValueDiscovered

/**
 * Detects messages that qualify as [UuidValue][io.spine.base.UuidValue].
 */
internal class UuidValueDiscovery : Policy<TypeDiscovered>(), UuidPluginComponent {

    private val settings: Uuids by lazy {
        loadSettings()
    }

    @React
    override fun whenever(
        @External event: TypeDiscovered
    ): EitherOf2<UuidValueDiscovered, NoReaction> {
        val type = event.type
        return if (type.isUuidValue()) {
            EitherOf2.withA(
                uuidValueDiscovered {
                    this@uuidValueDiscovered.type = type
                    settings = this@UuidValueDiscovery.settings
                }
            )
        } else {
            EitherOf2.withB(nothing())
        }
    }
}

/**
 * Tells if this message type qualifies as [UuidValue][io.spine.base.UuidValue].
 */
private fun MessageType.isUuidValue(): Boolean {
    val oneField = fieldList.size == 1
    if (!oneField) {
        return false
    }
    val field = fieldList.first()
    val isString = field.type.primitive == PrimitiveType.TYPE_STRING
    val nameMatches = field.name.value == "uuid"
    return isString && nameMatches
}
