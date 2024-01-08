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
package io.spine.tools.mc.java.annotation

import io.spine.code.proto.FileName

/**
 * Test proto file names.
 *
 * See `resources/annotator-plugin-test/src/main/proto/` directory for the files.
 */
enum class GivenProtoFile(value: String) {
    NO_INTERNAL_OPTIONS("no_internal_options.proto"),
    NO_INTERNAL_OPTIONS_MULTIPLE("no_internal_options_multiple.proto"),
    OUTER_INTERNAL("outer_internal.proto"),
    INTERNAL_ALL_SERVICE("internal_all_service.proto"),
    INTERNAL_ALL_MULTIPLE("internal_all_multiple.proto"),
    INTERNAL_MESSAGE("internal_message.proto"),
    INTERNAL_MESSAGE_MULTIPLE("internal_message_multiple.proto"),
    INTERNAL_FIELD("internal_field.proto"),
    INTERNAL_FIELD_MULTIPLE("internal_field_multiple.proto"),
    SPI_SERVICE("spi_service.proto"),
    SPI_ALL("spi_all.proto"),
    DUPLICATION("duplication.proto"),
    REVERTING("reverting.proto");

    val fileName: FileName = FileName.of(value)
}
