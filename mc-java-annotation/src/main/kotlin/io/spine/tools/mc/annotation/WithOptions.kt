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

import com.google.protobuf.BoolValue
import io.spine.protodata.Option
import io.spine.tools.mc.annotation.event.FileOptionMatched

/**
 * Common interface for views that deal with API options.
 */
public interface WithOptions {

    public fun getOptionList(): List<Option>

    /**
     * Checks if an API option is set on the file level and
     * is reverted on the type or `assumed` level.
     */
    public fun revertsFileWide(option: FileOptionMatched): Boolean {
        val alreadySetOption = getOptionList().find {
            it.name == option.assumed.name
        }
        alreadySetOption?.let {
            // File-wide options are only handled and matched iff they set to `true`.
            //
            // If the type has an option set to `false`, it means that the file-wide
            // option is explicitly reverted.
            return !(it.value.unpack(BoolValue::class.java).value)
        }
        return false
    }
}
