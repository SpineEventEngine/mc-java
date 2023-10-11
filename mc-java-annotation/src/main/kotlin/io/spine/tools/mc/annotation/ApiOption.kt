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
import io.spine.protobuf.pack
import io.spine.protodata.Option
import io.spine.protodata.option
import com.google.protobuf.Any as ProtoAny

/**
 * This object simulates an enum of options, which are used to mark API elements.
 *
 * We use this arrangement to allow lazy instantiation of elements, also avoiding
 * boilerplate code of enum declaration in this particular case.
 */
internal enum class ApiOption(optionName: String) {

    INTERNAL_ALL("internal_all"),
    SPI_ALL("SPI_all"),
    EXPERIMENTAL_API("experimental_all"),
    BETA_ALL("beta_all");

    internal val option: Option = option {
        name = optionName
        value = trueValue
    }

    private val trueValue: ProtoAny by lazy {
        BoolValue.of(true).pack()
    }
}

internal fun ApiOption.contains(option: Option): Boolean =
    enumValues<ApiOption>().any { it.option == option }

internal fun ApiOption.contains(option: String): Boolean =
    enumValues<ApiOption>().any { it.option.name == option }
