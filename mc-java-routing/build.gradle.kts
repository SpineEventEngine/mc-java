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

import io.spine.dependency.build.Ksp
import io.spine.dependency.lib.AutoService
import io.spine.dependency.lib.AutoServiceKsp
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.local.CoreJava
import io.spine.dependency.local.Logging
import io.spine.dependency.test.Kotest
import io.spine.dependency.test.KotlinCompileTesting

plugins {
    kotlin("jvm")
    ksp
    id("io.spine.mc-java")
}

dependencies {
    ksp(AutoServiceKsp.processor)
    implementation(kotlin("stdlib"))
    implementation(Ksp.symbolProcessingApi)
    implementation(KotlinPoet.lib)
    implementation(KotlinPoet.ksp)
    implementation(CoreJava.server)
    implementation(AutoService.annotations)

    testImplementation(Kotest.assertions)
    testImplementation(KotlinCompileTesting.libKsp)
    testImplementation(Logging.testLib)
}

configurations
    // https://detekt.dev/docs/gettingstarted/gradle/#dependencies
    .matching { it.name != "detekt" }
    .all {
    resolutionStrategy {
        force(
            Ksp.symbolProcessingApi,
            Ksp.symbolProcessing,
            Kotlin.Compiler.embeddable,
        )
    }
}

// Avoid the missing file error for generated code when running tests out of IDE.
afterEvaluate {
    val kspTestKotlin by tasks.getting
    val launchTestProtoData by tasks.getting
    kspTestKotlin.dependsOn(launchTestProtoData)
}
