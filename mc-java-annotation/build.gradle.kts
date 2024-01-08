/*
 * Copyright 2023, TeamDev. All rights reserved.
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

@file:Suppress("RemoveRedundantQualifierName")

import io.spine.internal.dependency.Roaster
import io.spine.internal.dependency.Spine

buildscript {
    standardSpineSdkRepositories()
    dependencies {
        classpath(io.spine.internal.dependency.Spine.McJava.pluginLib)
    }
}

plugins {
    `java-test-fixtures`
}

apply(plugin = "io.spine.mc-java")

dependencies {
    val guavaGroup = "com.google.guava"
    implementation(Roaster.api) {
        exclude(group = guavaGroup)
    }
    implementation(Roaster.jdt) {
        exclude(group = guavaGroup)
    }

    implementation(project(":mc-java-base"))
    implementation(Spine.server)
    implementation(Spine.Logging.lib)

    testFixturesImplementation(Spine.toolBase)
    testFixturesImplementation(Spine.testlib)
    testFixturesImplementation(Roaster.api) {
        exclude(group = guavaGroup)
    }
    testFixturesImplementation(Roaster.jdt) {
        exclude(group = guavaGroup)
    }

    testImplementation(Spine.pluginTestlib)
    testImplementation(gradleTestKit())
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

/*
 * Disable the generation of rejections because:
 *  1. We don't have rejections in this code.
 *  2. We want to avoid errors that may be caused by the code which has not yet
 *     fully migrated to the latest ProtoData API.
 */
//modelCompiler {
//    java {
//        codegen {
//            rejections().enabled.set(false)
//        }
//    }
//}
