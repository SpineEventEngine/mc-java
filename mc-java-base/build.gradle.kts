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

import io.spine.dependency.local.Logging
import io.spine.dependency.local.ProtoData
import io.spine.dependency.local.Spine
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation

plugins {
    id("io.spine.mc-java")
    `java-test-fixtures`
}

dependencies {
    compileOnlyApi(gradleApi())
    compileOnlyApi(gradleKotlinDsl())

    val apiDeps = arrayOf(
        Logging.lib,
        Spine.modelCompiler,
        ProtoData.java,
        Validation.config,
        ToolBase.pluginBase
    )
    apiDeps.forEach {
        api(it) {
            excludeSpineBase()
        }
    }
    api(Spine.base)

    arrayOf(
        Spine.base,
        gradleTestKit() /* for creating a Gradle project. */,
        Spine.testlib,
        ProtoData.testlib /* `PipelineSetup` API. */
    ).forEach {
        // Expose using API level for the submodules.
        testFixturesApi(it)
    }

    testImplementation(Spine.testlib)
    testImplementation(gradleTestKit())
    testImplementation(ToolBase.pluginTestlib)
}

forceSpineBase()

project.afterEvaluate {
    (tasks.getByName("sourcesJar") as Jar).apply {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

/*
 * Disable the generation of rejections because:
 *  1. We don't have rejections in this code.
 *  2. We want to avoid errors that may be caused by the code which has not yet
 *     fully migrated to the latest ProtoData API.
 */
modelCompiler {
    java {
        codegen {
            rejections.enabled.set(false)
        }
    }
}
