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

package io.spine.tools.mc.java.protoc.message;

import io.spine.protodata.FilePattern;
import io.spine.protodata.FilePatternFactory;
import io.spine.tools.java.code.JavaClassName;
import io.spine.tools.mc.java.protoc.given.TestInterface;
import io.spine.tools.protoc.plugin.message.tests.ProjectCreated;
import io.spine.type.MessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.tools.java.code.Names.className;

@DisplayName("`GenerateInterfaces` should")
final class InterfacesTest {

    @DisplayName("implement interface")
    @Test
    void implementInterface() {
        var pattern = FilePatternFactory.INSTANCE.suffix("test_events.proto");
        var className = className(TestInterface.class);
        var implementByPattern = newTask(className, pattern);
        var targetType = new MessageType(ProjectCreated.getDescriptor());
        assertThat(implementByPattern.generateFor(targetType))
                .isNotEmpty();
    }

    private static ImplementByPattern newTask(JavaClassName className, FilePattern pattern) {
        return new ImplementByPattern(className, pattern);
    }
}
