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
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.given.devices.Device
import io.spine.server.route.Route
import java.io.File
import kotlin.jvm.java
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
        val serverJar = File(Route::class.java.protectionDomain.codeSource.location.path)
        val compiledProtos = File(Device::class.java.protectionDomain.codeSource.location.path)

        compilation.apply {
            javaPackagePrefix = "io.spine.routing.given"
            symbolProcessorProviders = listOf(RouteProcessorProvider())
            classpaths = classpaths + listOf(
                serverJar,
                compiledProtos
            )
        }
    }

    @Test
    fun `when a non-static method is annotated`() {
        compilation.apply {
            sources = listOf(annotatedNonStatic)
        }
        val result = compilation.compile()

        result.exitCode shouldBe COMPILATION_ERROR
        result.messages.let {
            it shouldContain "`route()`" // The name of the method in error.
            it shouldContain "`@Route`"  // The reference to the annotation.
            it shouldContain "must be `static`." // The nature of the error.
        }
    }
}

private fun named(simpleName: String): String = "io/spine/given/devices/${simpleName}.java"

@Suppress("ClassNameDiffersFromFileName", "MissingPackageInfo")
private val annotatedNonStatic = SourceFile.java(
    named("AnnotatedNonStatic"), """
    package io.spine.given.devices;
    
    import io.spine.given.devices.events.StatusReported;    
    import io.spine.server.projection.Projection;
    import io.spine.server.route.Route;
        
    class AnnotatedNonStatic extends Projection<DeviceId, DeviceStatus, DeviceStatus.Builder> {
                
        // Error: The method must be static.
        @Route
        DeviceId route(StatusReported event) {
            return event.getDevice();
        }
    }    
    """.trimIndent()
)
