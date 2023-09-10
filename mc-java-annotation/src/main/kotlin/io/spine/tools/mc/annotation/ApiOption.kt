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
import io.spine.annotation.Beta
import io.spine.annotation.Experimental
import io.spine.annotation.Internal
import io.spine.annotation.SPI as Spi
import io.spine.protobuf.pack
import io.spine.protodata.Option
import io.spine.protodata.option
import kotlin.reflect.KClass

internal enum class ApiOption(
    val fileOption: Option,
    val messageOption: Option,
    val enumOption: Option? = null,
    val serviceOption: Option? = null,
    val annotationClass: KClass<out Annotation>
) {

    BETA(
        fileOption = option("beta_all"),
        messageOption = option("beta"),
        annotationClass = Beta::class
    ),

    EXPERIMENTAL(
        fileOption = option("experimental_all"),
        messageOption = option("experimental_type"),
        annotationClass = Experimental::class
    ),

    INTERNAL(
        fileOption = option("internal_all"),
        messageOption = option("internal"),
        annotationClass = Internal::class,
    ),

    SPI(
        fileOption = option("SPI_all"),
        messageOption = option("SPI_type"),
        serviceOption = option("SPI_Service"),
        annotationClass = Spi::class
    );

    companion object {
        fun findMatching(fileOption: Option): ApiOption? {
            return values().find { it.fileOption == fileOption }
        }
    }
}

/**
 * Creates an option with the given name and `true` value.
 *
 * We set the value to `true` because setting an API option to `false` does
 * not make much sense and is equivalent to not setting it at all.
 */
private fun option(name: String): Option = option {
    this.name = name
    value = BoolValue.of(true).pack()
}
