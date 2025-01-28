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

import io.spine.dependency.local.CoreJava

plugins {
    kotlin("jvm")
    ksp
    `java-test-fixtures`
    id("io.spine.mc-java")
}

dependencies {
    testImplementation(kotlin("stdlib"))
    kspTest(project(":mc-java-routing"))
    kspTestFixtures(project(":mc-java-routing"))
    testFixturesImplementation(CoreJava.server)
    
    testFixturesImplementation(project(":mc-java-routing"))?.because(
        "We need this dependency temporarily, until the interfaces defined" +
                " in the package `io.spine.server.route` are moved to CoreJava."
    )
}
                                                                    
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
    sourceSets.testFixtures {
        kotlin.srcDir("build/generated/ksp/testFixtures/kotlin")
    }
}

// Avoid Gradle warning on disabled execution optimization because of the absence of
// explicit or implicit dependencies.
afterEvaluate {
    val kspTestFixturesKotlin by tasks.getting
    val launchTestFixturesProtoData by tasks.getting
    kspTestFixturesKotlin.dependsOn(launchTestFixturesProtoData)
}
