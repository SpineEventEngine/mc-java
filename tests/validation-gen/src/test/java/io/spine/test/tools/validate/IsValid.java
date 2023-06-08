/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.test.tools.validate;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidationException;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.type.Json.toJson;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Assertions related to message validness.
 */
final class IsValid {

    /**
     * Prevents the utility class instantiation.
     */
    private IsValid() {
    }

    /**
     * Assert the given {@code builder} produces a valid message.
     *
     * @param builder
     *         the message builder
     */
    static void assertValid(Message.Builder builder) {
        var msg = builder.build();
        assertThat(msg)
                .isNotNull();
    }

    /**
     * Assert the given {@code builder} produces an invalid message.
     *
     * @param builder
     *         the message builder
     * @return the violations received from building the message
     */
    @CanIgnoreReturnValue
    static List<ConstraintViolation> assertInvalid(Message.Builder builder) {
        try {
            var msg = builder.build();
            return fail(format("Expected an invalid message but got: %s", toJson(msg)));
        } catch (ValidationException e) {
            return e.getConstraintViolations();
        }
    }
}
