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

package io.spine.tools.mc.java.protoc.field;

import com.google.common.testing.NullPointerTester;
import io.spine.base.SubscribableField;
import io.spine.protodata.FilePatternFactory;
import io.spine.tools.mc.java.settings.CodegenSettings;
import io.spine.tools.mc.java.settings.GenerateFields;
import io.spine.tools.mc.java.settings.GroupSettings;
import io.spine.tools.mc.java.settings.MessageGroup;
import io.spine.tools.mc.java.settings.Pattern;
import io.spine.tools.protoc.plugin.nested.Task;
import io.spine.tools.protoc.plugin.nested.TaskView;
import io.spine.type.EnumType;
import io.spine.type.MessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.tools.java.code.Names.className;

@DisplayName("`FieldGenerator` should")
class FieldGenTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(FieldGen.class);
    }

    @Test
    @DisplayName("generate code for message types where appropriate")
    void generateCodeForMessages() {
        var config = newConfig();

        var generator = FieldGen.instance(config);
        var type = new MessageType(TaskView.getDescriptor());
        var output = generator.generate(type);

        assertThat(output)
                .isNotEmpty();
    }

    @Test
    @DisplayName("ignore non-`Message` types")
    void ignoreNonMessageTypes() {
        var config = newConfig();

        var generator = FieldGen.instance(config);
        var enumType = EnumType.create(Task.Priority.getDescriptor());
        var output = generator.generate(enumType);

        assertThat(output)
                .isEmpty();
    }

    private static CodegenSettings newConfig() {
        var messages = MessageGroup.newBuilder();
        messages.setPattern(
                Pattern.newBuilder().setFile(FilePatternFactory.INSTANCE.suffix("test_fields.proto")));
        var generateFields = GenerateFields.newBuilder()
                .setSuperclass(className(SubscribableField.class))
                .build();
        messages.setGenerateFields(generateFields);
        var groupSettings = GroupSettings.newBuilder()
                .addGroup(messages.build())
                .build();
        return CodegenSettings.newBuilder()
                .setGroupSettings(groupSettings)
                .build();
    }
}
