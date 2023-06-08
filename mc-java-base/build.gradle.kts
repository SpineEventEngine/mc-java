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

import io.spine.internal.dependency.ProtoData
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Validation

dependencies {
    /* Use `implementation` dependency on `gradleApi()` to make PMD code analysis see
       Gradle API classes. Otherwise, it should have been `compileOnlyApi` since Gradle
       executes this code and its API is automatically provided. */
    implementation(gradleApi())
    compileOnlyApi(gradleKotlinDsl())

    api(Spine.logging)
    api(Spine.modelCompiler)
    api(ProtoData.codegenJava)
    api(Validation.config)
    api(Validation.runtime)
    api(Spine.pluginBase)

    testImplementation(Spine.testlib)
    testImplementation(gradleTestKit())
    testImplementation(Spine.pluginTestlib)
}

project.afterEvaluate {
    (tasks.getByName("sourcesJar") as Jar).apply {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
