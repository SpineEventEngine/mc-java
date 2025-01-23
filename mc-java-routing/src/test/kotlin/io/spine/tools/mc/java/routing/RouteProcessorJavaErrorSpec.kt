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

@file:Suppress(
    "ClassNameDiffersFromFileName" /* false positive in IDEA */,
    "MissingPackageInfo" /* don't need them for these tests. */
)

package io.spine.tools.mc.java.routing

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.base.CommandMessage
import io.spine.core.CommandContext
import io.spine.core.EventContext
import io.spine.given.devices.Device
import io.spine.server.route.Route
import io.spine.string.simply
import io.spine.tools.mc.java.routing.RouteSignature.Companion.routeRef
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCompilerApi
@DisplayName("`RouteProcessor` should detect Java code errors")
internal class RouteProcessorJavaErrorSpec {

    private lateinit var compilation: KotlinCompilation

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

    /*
     * Error: non-static method.
     */
    private val nonStatic = javaFile("NonStatic", """
        
    package io.spine.given.devices;
    
    import io.spine.given.devices.events.StatusReported;    
    import io.spine.server.projection.Projection;
    import io.spine.server.route.Route;
        
    class NonStatic extends Projection<DeviceId, DeviceStatus, DeviceStatus.Builder> {
        @Route
        DeviceId route(StatusReported event) {
            return event.getDevice();
        }
    }
    """.trimIndent())

    @Test
    fun `when a non-static method is annotated`() {
        compilation.apply {
            sources = listOf(nonStatic)
        }
        val result = compilation.compile()

        result.exitCode shouldBe COMPILATION_ERROR
        result.messages.let {
            it shouldContain "The method `route()`"
            it shouldContain routeRef
            it shouldContain "must be `static`."
        }
    }

    /*
     * Error: no parameters.
     */
    private val noParameters = javaFile("NoParameters", """
        
    package io.spine.given.devices;
    
    import io.spine.given.devices.events.StatusReported;    
    import io.spine.server.projection.Projection;
    import io.spine.server.route.Route;
        
    class NoParameters extends Projection<DeviceId, DeviceStatus, DeviceStatus.Builder> {
        @Route
        static DeviceId route() {
            return DeviceId.generate();
        }
    }
    """.trimIndent())

    @Test
    fun `when no parameters are specified`() {
        compilation.apply {
            sources = listOf(noParameters)
        }
        val result = compilation.compile()
        result.exitCode shouldBe COMPILATION_ERROR
        result.messages.let {
            it shouldContain "The method `route()`"
            it shouldContain routeRef
            it shouldContain "one or two parameters. Encountered: 0."
        }
    }

    /*
     * Error: too many parameters.
     */
    private val tooManyParameters = javaFile("TooManyParameters", """
        
    package io.spine.given.devices;
        
    import io.spine.core.EventContext;
    import io.spine.given.devices.events.StatusReported;    
    import io.spine.server.projection.Projection;
    import io.spine.server.route.Route;
        
    class TooManyParameters extends Projection<DeviceId, DeviceStatus, DeviceStatus.Builder> {
        @Route
        static DeviceId route(StatusReported event, EventContext context, Object other) {
            return event.getDevice();
        }
    }
    """.trimIndent())

    @Test
    fun `when too many parameters are specified`() {
        compilation.apply {
            sources = listOf(tooManyParameters)
        }
        val result = compilation.compile()
        result.exitCode shouldBe COMPILATION_ERROR
        result.messages.let {
            it shouldContain "The method `route()`"
            it shouldContain routeRef
            it shouldContain "one or two parameters. Encountered: 3."
        }
    }

    /**
     * Error: the second parameter is [io.spine.core.CommandContext] instead of [EventContext].
     */
    private val wrongSecondParameter = javaFile("WrongSecondParameter", """
    
    package io.spine.given.devices;
        
    import io.spine.core.CommandContext;
    import io.spine.given.devices.events.StatusReported;
    import io.spine.server.projection.Projection;
    import io.spine.server.route.Route;
        
    class WrongSecondParameter extends Projection<DeviceId, DeviceStatus, DeviceStatus.Builder> {
        @Route
        static DeviceId route(StatusReported event, CommandContext context) {
            return event.getDevice();
        }
    }
    """.trimIndent()
    )

    @Test
    fun `when the second parameters is of incorrect type`() {
        compilation.apply {
            sources = listOf(wrongSecondParameter)
        }
        val result = compilation.compile()
        result.exitCode shouldBe COMPILATION_ERROR
        result.messages.let {
            it shouldContain "The second parameter of the method `route()`"
            it shouldContain routeRef
            it shouldContain "must be `${simply<EventContext>()}`."
            it shouldContain "Encountered: `${simply<CommandContext>()}`."
        }
    }
}

