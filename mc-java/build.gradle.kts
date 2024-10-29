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

import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.ProtoData
import io.spine.dependency.local.Spine
import io.spine.dependency.local.Validation
import io.spine.gradle.WriteVersions

dependencies {
    implementation(ProtoData.pluginLib)

    // We access the Protobuf Gradle Plugin extension, so we need it as a dependency.
    implementation(Protobuf.GradlePlugin.lib)

    // Module dependencies
    listOf(
        ":mc-java-base",
        ":mc-java-annotation",
        ":mc-java-checks",
        ":mc-java-entity",
        ":mc-java-signal",
        ":mc-java-marker",
        ":mc-java-message-group",
        ":mc-java-uuid",
        ":mc-java-comparable",
    ).forEach {
        implementation(project(it))
    }

    // Test dependencies
    listOf(
        gradleApi(),
        gradleKotlinDsl(),
        gradleTestKit(),
        Spine.testlib,
        Spine.pluginTestlib,
        testFixtures(project(":mc-java-base"))
    ).forEach {
        testImplementation(it)
    }
}

tasks {
    /**
     * Tests use the artifacts published to `mavenLocal`, so we need to publish them all first.
     */
    test {
        dependsOn(rootProject.tasks.named("localPublish"))
    }

    withType<WriteVersions>().configureEach {

        // Store the version of gRPC so that we can set the artifact for `protoc`.
        // See `io.spine.tools.mc.java.gradle.plugins.JavaProtocConfigurationPlugin` for details.
        version(Grpc.ProtocPlugin.artifact)

        // Store the version of Validation so that we can add the dependency for
        // the `protoData` configuration.
        // See `io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigurationPlugin` for details.
        version(Validation.java)

        // Store the version of `tool-base` so that we can add the dependency for
        // the `protoData` configuration when configuring rejection codegen.
        // See `io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigurationPlugin` for details.
        version(Spine.toolBase)
    }
}
