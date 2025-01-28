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

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.tools.mc.java.routing.RouteSignature.Companion.jvmStaticRef
import io.spine.tools.mc.java.routing.RouteSignature.Companion.routeRef
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCompilerApi
@DisplayName("`RouteProcessor` should detect Kotlin code errors")
internal class KotlinRouteErrorSpec : RouteCompilationTest() {

    /**
     * Error: The function must be a static method of a class.
     */
    private val fileLevelFunction =  kotlinFile("FileLevelFunction", """
        
    package io.spine.given.devices
    
    import io.spine.base.EventMessage
    import io.spine.server.route.Route
    
    @Route
    private fun route(e: EventMessage): String = "Hello" 
    """.trimIndent())

    @Test
    fun `when a function is defined on a file level`() {
        compilation.apply {
            sources = listOf(fileLevelFunction)
        }

        val result = compilation.compileSilently()

        result.exitCode shouldBe COMPILATION_ERROR
        result.messages.let {
            it shouldContain "`route()`" // The name of the function in error.
            it shouldContain routeRef
            it shouldContain "a member of a companion object of an entity class"
            it shouldContain jvmStaticRef
        }
    }

    /**
     * Error: The method must be annotated as `JvmStatic`.
     */
    private val notJvmStatic = kotlinFile("NotJvmStatic", """
    package io.spine.given.devices
    
    import io.spine.given.devices.events.StatusReported
    import io.spine.server.projection.Projection
    import io.spine.server.route.Route
        
    class NotJvmStatic : Projection<DeviceId, DeviceStatus, DeviceStatus.Builder>() {
                    
        companion object {
            @Route
            fun route(e: StatusReported): DeviceId {
                return event.getDevice()
            }
        }
    }
    """.trimIndent())

    @Test
    fun `when a function is not 'JvmStatic''`() {
        compilation.apply {
            sources = listOf(notJvmStatic)
        }

        val result = compilation.compileSilently()

        result.exitCode shouldBe COMPILATION_ERROR
        result.messages.let {
            it shouldContain "`route()`" // The name of the function in error.
            it shouldContain routeRef
            it shouldContain "a member of a companion object and annotated with"
            it shouldContain jvmStaticRef
        }
    }

    /**
     * Error: The method must belong to a companion object.
     */
    private val notCompanionMember = kotlinFile("NotCompanionMember", """
    package io.spine.given.devices
    
    import io.spine.given.devices.events.StatusReported
    import io.spine.server.projection.Projection
    import io.spine.server.route.Route
        
    class NotCompanionMember : Projection<DeviceId, DeviceStatus, DeviceStatus.Builder>() {
        @Route
        fun route(e: StatusReported): DeviceId {
            return event.getDevice()
        }
    }
    """.trimIndent())

    @Test
    fun `when a function is not a member of a companion object`() {
        compilation.apply {
            sources = listOf(notCompanionMember)
        }

        val result = compilation.compileSilently()

        result.exitCode shouldBe COMPILATION_ERROR
        result.messages.let {
            it shouldContain "`route()`" // The name of the function in error.
            it shouldContain routeRef
            // The error is the same as for not annotated as `JvmStatic`.
            it shouldContain "a member of a companion object and annotated with"
            it shouldContain jvmStaticRef
        }
    }

    /**
     * Correct routing method.
     */
    private val companionMember = kotlinFile("CompanionMember", """
    package io.spine.given.devices
    
    import io.spine.given.devices.events.StatusReported
    import io.spine.server.projection.Projection
    import io.spine.server.route.Route
        
    class CompanionMember : Projection<DeviceId, DeviceStatus, DeviceStatus.Builder>() {
    
        companion object {
            @Route
            @JvmStatic
            fun route(e: StatusReported): DeviceId {
                return event.getDevice()
            }
        }
    }
    """.trimIndent())

    @Test
    fun `accept a function defined in a companion object`() {
        compilation.apply {
            sources = listOf(companionMember)
        }
        val result = compilation.compileSilently()
        result.exitCode shouldBe ExitCode.OK
    }
}
