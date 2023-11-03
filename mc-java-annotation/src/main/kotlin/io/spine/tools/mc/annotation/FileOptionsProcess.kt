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

@file:Suppress("UNUSED_PARAMETER")

package io.spine.tools.mc.annotation

import com.google.protobuf.BoolValue
import com.google.protobuf.kotlin.isA
import com.google.protobuf.kotlin.unpack
import io.spine.base.EventMessage
import io.spine.core.External
import io.spine.protodata.FilePath
import io.spine.protodata.Option
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.event.FileEntered
import io.spine.protodata.event.FileExited
import io.spine.protodata.event.FileOptionDiscovered
import io.spine.protodata.event.serviceOptionDiscovered
import io.spine.protodata.event.typeOptionDiscovered
import io.spine.server.entity.alter
import io.spine.server.entity.state
import io.spine.server.event.React
import io.spine.server.procman.ProcessManager
import io.spine.tools.mc.annotation.ApiOption.Companion.findMatching
import io.spine.server.model.Nothing as NoEvents

internal class FileOptionsProcess : ProcessManager<FilePath, FileOptions, FileOptions.Builder>() {

    /**
     * Adds the API options from the file to the state of this process IFF their
     * values are set to `true`.
     */
    @React
    fun on(@External e: FileEntered): NoEvents {
        alter {
            file = e.file.path
        }
        return nothing()
    }

    @React
    fun on(@External e: FileOptionDiscovered): NoEvents {
        val optionValue = e.option.value
        val isApiLevelOption = findMatching(e.option) != null
        if (optionValue.isA<BoolValue>() && isApiLevelOption
            && optionValue.unpack<BoolValue>().value // is `true`
        ) alter {
            addOption(e.option)
        }
        return nothing()
    }

    @React
    fun on(@External e: FileExited): Iterable<EventMessage> {
        if (state.optionList.isEmpty()) {
            // There are no API-related options in this file.
            return emptyList()
        }
        return emitEvents()
    }

    private fun emitEvents(): Iterable<EventMessage> {
        val currentFile = state().file
        val protoSrc = select(ProtobufSourceFile::class.java).findById(currentFile)
        check(protoSrc != null) {
            "Unable to load type data of the Protobuf source file with path `$currentFile`."
        }
        val events = mutableListOf<EventMessage>()

        state.optionList.forEach { option ->
            val apiOption = findMatching(option)
            apiOption?.let {
                val messageOption = it.messageOption
                protoSrc.addMessageEvents(events, messageOption)
                it.serviceOption?.let {
                    protoSrc.addServiceEvents(events, it)
                }
            }
        }
        return events
    }
}

private fun ProtobufSourceFile.addMessageEvents(
    events: MutableList<EventMessage>,
    typeOption: Option
) {
    typeMap.values.forEach {
        events.add(typeOptionDiscovered {
            file = filePath
            type = it.name
            option = typeOption
        })
    }
}

private fun ProtobufSourceFile.addServiceEvents(
    events: MutableList<EventMessage>,
    serviceOption: Option
) {
    serviceMap.values.forEach {
        events.add(serviceOptionDiscovered {
            file = filePath
            service = it.name
            option = serviceOption
        })
    }
}

