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

/**
 * This object simulates an enum of options, which are used to mark API elements.
 *
 * We use this arrangement to allow lazy instantiation of elements, also avoiding
 * boilerplate code of enum declaration in this particular case.
 */
internal object ApiOption {

    val internalAll: Option by lazy {
        option("internal_all")
    }

    val spiAll: Option by lazy {
        option("SPI_all")
    }

    val experimentalApi: Option by lazy {
        option("experimental_all")
    }

    val betaAll: Option by lazy {
        option("beta_all")
    }

    val values: List<Option> by lazy {
        listOf(internalAll, spiAll, experimentalApi, betaAll)
    }
}

internal fun ApiOption.contains(option: Option): Boolean =
    values.contains(option)

/**
 * Creates an option with the given name and `true` value.
 */
private fun option(name: String): Option = option {
    this.name = name
    value = BoolValue.of(true).pack()
}
