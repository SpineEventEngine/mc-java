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

package io.spine.tools.mc.annotation

import com.google.protobuf.BoolValue
import com.google.protobuf.kotlin.isA
import com.google.protobuf.kotlin.unpack
import io.spine.annotation.Beta
import io.spine.annotation.Experimental
import io.spine.annotation.Internal
import io.spine.protobuf.pack
import io.spine.protodata.Option
import io.spine.protodata.Types
import io.spine.protodata.option
import com.google.protobuf.Any as ProtoAny
import io.spine.annotation.SPI as Spi

/**
 * An option that can be set on a `.proto` file, message, field, or service to
 * designate the API stability level of the corresponding element.
 *
 * The options are defined in the `options.proto` file.
 * This enum associates the options with the corresponding Java annotation classes.
 */
internal enum class ApiOption(
    val fileOption: Option,
    val messageOption: Option,
    val fieldOption: Option?,
    val serviceOption: Option?,
    val annotationClass: Class<out Annotation>
) {

    BETA(
        fileOption = option("beta_all"),
        messageOption = option("beta_type"),
        fieldOption = option("beta"),
        serviceOption = null,
        annotationClass = Beta::class.java
    ),

    EXPERIMENTAL(
        fileOption = option("experimental_all"),
        messageOption = option("experimental_type"),
        fieldOption = option("experimental"),
        serviceOption = null,
        annotationClass = Experimental::class.java
    ),

    INTERNAL(
        fileOption = option("internal_all"),
        messageOption = option("internal_type"),
        fieldOption = option("internal"),
        serviceOption = null,
        annotationClass = Internal::class.java,
    ),

    SPI(
        fileOption = option("SPI_all"),
        messageOption = option("SPI_type"),
        fieldOption = null,
        serviceOption = option("SPI_service"),
        annotationClass = Spi::class.java
    );

    companion object {

        /**
         * Finds an [ApiOption] matching the given [Option] by its name.
         */
        fun findMatching(option: Option): ApiOption? {
            val optionName = option.name
            return values().find {
                it.fileOption.name == optionName ||
                it.messageOption.name == optionName ||
                it.fieldOption?.name == optionName ||
                it.serviceOption?.name == optionName
            }
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
    type = Types.boolean
    value = BoolValue.of(true).pack()
}

/**
 * Tells if the given option is set to `true`.
 */
internal fun ProtoAny.isTrue(): Boolean = isA<BoolValue>() && unpack<BoolValue>().value
