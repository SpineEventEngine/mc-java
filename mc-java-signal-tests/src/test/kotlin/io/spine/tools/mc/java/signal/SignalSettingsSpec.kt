/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.tools.mc.java.signal

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.spine.tools.java.reference
import io.spine.tools.mc.java.gradle.settings.CodegenSettings
import java.io.File
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * This is a test suite for [io.spine.tools.mc.java.gradle.settings.SignalSettings] class
 * which belongs to `mc-java-base` module.
 *
 * We have this test suite in another module to check the correctness of default settings
 * specified as strings against classes of this module.
 */
@DisplayName("`SignalSettings` should")
internal class SignalSettingsSpec {

    private lateinit var project: Project
    private lateinit var codegenSettings: CodegenSettings

    @BeforeEach
    fun createProject(@TempDir projectDir: File) {
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        codegenSettings = CodegenSettings(project)
    }

    @Test
    @Disabled("Until command message interface is implemented via an aciton")
    fun `provide default actions for command messages`() {
        codegenSettings.commands.toProto().actionList shouldContainExactlyInAnyOrder listOf(
            //TODO:2024-07-30:alexander.yevsyukov: Add interface action.
        )
    }

    @Test
    fun `provide default actions for event messages`() {
        codegenSettings.events.toProto().actionList shouldContainExactlyInAnyOrder listOf(
            AddEventMessageField::class.java.reference
        )
    }

    @Test
    fun `provide default actions for rejection messages`() {
        codegenSettings.events.toProto().actionList shouldContainExactlyInAnyOrder listOf(
            AddEventMessageField::class.java.reference
        )
    }
}
