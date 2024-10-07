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

package io.spine.tools.mc.java.comparable

import io.spine.protodata.plugin.Plugin
import io.spine.protodata.plugin.Policy
import io.spine.protodata.plugin.View
import io.spine.protodata.render.Renderer

/**
 * Looks for messages with `compare_by` option and applies render actions specified in
 * [CodegenSettings][io.spine.tools.mc.java.gradle.settings.CodegenSettings.forComparables].
 *
 * The default list of actions is configured in
 * [ComparableSettings][io.spine.tools.mc.java.gradle.settings.ComparableSettings].
 */
public class ComparablePlugin : Plugin {

    override fun policies(): Set<Policy<*>> = setOf(
        ComparableMessageDiscovery()
    )

    override fun views(): Set<Class<out View<*, *, *>>> = setOf(
        ComparableMessageView::class.java
    )

    override fun renderers(): List<Renderer<*>> = listOf(
        ComparableActionsRenderer()
    )

    public companion object {

        /**
         * Settings ID for this plugin.
         */
        public val SETTINGS_ID: String = ComparablePlugin::class.java.canonicalName
    }
}
