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

package io.spine.tools.mc.java.protoc;

import io.spine.tools.protoc.plugin.EnhancedWithCodeGeneration;
import io.spine.type.MessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

@DisplayName("`InsertionPoint` should")
final class InsertionPointTest {

    @DisplayName("create valid Protoc insertion point")
    @ParameterizedTest(name = "\"{0}\"")
    @EnumSource(value = InsertionPoint.class, names = "outer_class_scope", mode = EXCLUDE)
    void createValidInsertionPoint(InsertionPoint insertionPoint) {
        var testMessage = new MessageType(EnhancedWithCodeGeneration.getDescriptor());
        var typeName = testMessage.name();
        var subject = assertThat(insertionPoint.forType(testMessage));
        subject.contains(typeName.toString());
        subject.contains(":");
        subject.contains(insertionPoint.getDefinition());
    }

    @DisplayName("create valid \"outer_class_scope\" insertion point")
    @Test
    void createValidOuterClassScopeInsertionPoint() {
        var actual = InsertionPoint.outer_class_scope.forType(null);
        assertEquals(InsertionPoint.outer_class_scope.getDefinition(), actual);
    }
}
