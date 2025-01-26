/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.routing

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.spine.base.CommandMessage
import io.spine.core.EventContext
import io.spine.given.devices.Device
import io.spine.server.route.Route
import kotlin.collections.plus
import org.junit.jupiter.api.BeforeEach

/**
 * Abstract base for tests checking handling errors using the [Route] annotation.
 *
 * The tests use types from the Protobuf code generated for the `given.devices` proto package.
 */
sealed class ErrorSpecTest {

    protected lateinit var compilation: KotlinCompilation

    @BeforeEach
    fun prepareCompilation() {
        compilation = KotlinCompilation()
        val baseJar = CommandMessage::class.java.classpathFile()
        val coreJar = EventContext::class.java.classpathFile()
        val serverJar = Route::class.java.classpathFile()
        val compiledProtos = Device::class.java.classpathFile()

        compilation.apply {
            javaPackagePrefix = "io.spine.routing.given"
            symbolProcessorProviders = listOf(RouteProcessorProvider())
            classpaths = classpaths + listOf(
                baseJar,
                coreJar,
                serverJar,
                compiledProtos
            )
        }
    }
}
