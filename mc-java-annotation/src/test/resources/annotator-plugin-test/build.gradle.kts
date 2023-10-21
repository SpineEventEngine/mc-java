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

import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.standardToSpineSdk
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.all
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.resolutionStrategy

buildscript {

    // NOTE: this file is copied from the root project in the test setup.
    apply(from = "$rootDir/test-env.gradle")
    apply(from = "${extra["enclosingRootDir"]}/version.gradle.kts")

    standardSpineSdkRepositories()

    val mcJavaVersion: String by extra
    dependencies {
        io.spine.internal.dependency.Protobuf.libs.forEach { classpath(it) }

        // Exclude `guava:18.0` as a transitive dependency by Protobuf Gradle plugin.
        classpath(io.spine.internal.dependency.Protobuf.GradlePlugin.lib) {
            exclude(group = "com.google.guava")
        }
        classpath(io.spine.internal.dependency.Spine.McJava.pluginLib(mcJavaVersion))
    }
}

plugins {
    java
}

allprojects {
    group = "io.spine.test"
    version = "3.14"
}

subprojects {

    apply(plugin = "java")
    apply(from = "$rootDir/test-env.gradle")

    val enclosingRootDir: String by extra
    apply {
        plugin("com.google.protobuf")
        plugin("io.spine.mc-java")
        from("${enclosingRootDir}/version.gradle.kts")
    }

    repositories.standardToSpineSdk()

    configurations {
        all {
            resolutionStrategy {
                force(
                    Spine.base,
                )
            }
        }
    }

    tasks.findByName("launchProtoData")?.apply {this as JavaExec
        debugOptions {
            // Set this option to `true` to enable remote debugging.
            enabled.set(false)
            port.set(5566)
            server.set(true)
            suspend.set(true)
        }
        System.err.println("Debug session for `LaunchProtoData` configured.")
    }

    dependencies {
        implementation(Spine.base)
        implementation(Grpc.stub)
        implementation(Grpc.protobuf)
    }

    sourceSets {
        main {
            java.srcDirs("$projectDir/generated/main/java", "$projectDir/generated/main/spine")
            resources.srcDir("$projectDir/generated/main/resources")
            (extensions.getByName("proto") as SourceDirectorySet).srcDir("$projectDir/src/main/proto")
        }
    }
}
