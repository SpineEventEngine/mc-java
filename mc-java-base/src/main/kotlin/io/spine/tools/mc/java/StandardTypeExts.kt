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

@file:JvmName("StandardTypes")

package io.spine.tools.mc.java

import java.nio.file.Path
import java.util.*
import kotlin.text.Charsets.UTF_8

/**
 * Converts this string to base64 encoded version using UTF-8 charset.
 */
@Deprecated("Please use the extension from the package `io.spine.string`.")
public fun String.toBase64Encoded(): String {
    val encoder = Base64.getEncoder()
    val valueBytes: ByteArray = toByteArray(UTF_8)
    return encoder.encodeToString(valueBytes)
}

/**
 * Decodes base64-encoded value into a string with UTF-8 charset.
 */
@Deprecated("Please use the extension from the package `io.spine.string`.")
public fun String.decodeBase64(): String {
    val decoder = Base64.getDecoder()
    val decodedBytes = decoder.decode(this)
    return String(decodedBytes, UTF_8)
}

/**
 * Converts this path to a base64-encoded string.
 *
 * @see [String.toBase64Encoded]
 */
@Deprecated("Please use the extension from the package `io.spine.io`.")
public fun Path.toBase64Encoded(): String = toString().toBase64Encoded()
