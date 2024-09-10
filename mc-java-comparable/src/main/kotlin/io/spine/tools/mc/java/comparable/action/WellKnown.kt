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

package io.spine.tools.mc.java.comparable.action

import com.google.protobuf.Duration
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Durations
import com.google.protobuf.util.Timestamps
import io.spine.protodata.MessageType
import io.spine.protodata.Type
import io.spine.protodata.isMessage
import io.spine.protodata.qualifiedName
import io.spine.protodata.typeName

/**
 * Enumerates well-known Protobuf types, which are allowed to be used in `compare_by` option.
 *
 * See also: [Protobuf Docs | Well-Known Types](https://protobuf.dev/reference/protobuf/google.protobuf/).
 */
internal object WellKnown {

    private val messages = mapOf(
        Duration::class.qualifiedName to "${Durations::class.qualifiedName}.comparator()",
        Timestamp::class.qualifiedName to "${Timestamps::class.qualifiedName}.comparator()",
    )

    private val values = listOf(
        "google.protobuf.BoolValue",
        "google.protobuf.DoubleValue",
        "google.protobuf.FloatValue",
        "google.protobuf.Int32Value",
        "google.protobuf.Int64Value",
        "google.protobuf.UInt32Value",
        "google.protobuf.UInt64Value",
        "google.protobuf.StringValue",
    )

    private val all = values + messages.keys

    /**
     * Returns a comparator for the given well-known [type].
     */
    fun comparatorFor(type: Type): String {
        require(type.isWellKnownMessage)
        return messages[type.typeName.qualifiedName]!!
    }

    /**
     * Tells if this [MessageType] is a well-known type eligible to participate in comparison.
     */
    val MessageType.isWellKnownComparable
        get() = all.contains(qualifiedName)

    /**
     * Tells if this [Type] is a well-known message type.
     */
    val Type.isWellKnownMessage
        get() = isMessage && messages.contains(typeName.qualifiedName)

    /**
     * Tells if this [Type] is a well-known value message type.
     */
    val Type.isWellKnownValue
        get() = isMessage && values.contains(typeName.qualifiedName)
}
