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

@file:Suppress("RemoveRedundantQualifierName") // To prevent IDEA replacing FQN imports.

import io.spine.dependency.build.ErrorProne
import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.KotlinX
import io.spine.dependency.local.Logging
import io.spine.dependency.local.ProtoData
import io.spine.dependency.local.Spine
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.dependency.test.JUnit
import io.spine.dependency.test.Truth
import io.spine.gradle.javac.configureErrorProne
import io.spine.gradle.javac.configureJavac
import io.spine.gradle.kotlin.setFreeCompilerArgs
import io.spine.gradle.publish.PublishingRepos.gitHub
import io.spine.gradle.standardToSpineSdk
import io.spine.gradle.testing.configureLogging
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {

    val baseRoot = "${rootDir}/.."
    val versionGradle = "${baseRoot}/version.gradle.kts"
    apply(from = versionGradle)

    standardSpineSdkRepositories()

    val mcJavaVersion: String by extra
    val protoData = io.spine.dependency.local.ProtoData
    dependencies {
        classpath(io.spine.dependency.lib.Guava.lib)
        classpath(io.spine.dependency.lib.Protobuf.GradlePlugin.lib) {
            exclude(group = "com.google.guava")
        }
        classpath(io.spine.dependency.build.ErrorProne.GradlePlugin.lib) {
            exclude(group = "com.google.guava")
        }
        classpath(protoData.pluginLib)
        classpath(io.spine.dependency.local.McJava.pluginLib(mcJavaVersion))
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:1.7.10-1.0.6")
    }

    with(configurations) {
        doForceVersions(this)
        val spine = io.spine.dependency.local.Spine
        val toolBase = io.spine.dependency.local.ToolBase
        val logging = io.spine.dependency.local.Logging
        val grpc = io.spine.dependency.lib.Grpc
        all {
            resolutionStrategy {
                force(
                    io.spine.dependency.lib.KotlinX.Coroutines.bom,
                    io.spine.dependency.lib.KotlinX.Coroutines.core,
                    io.spine.dependency.lib.KotlinX.Coroutines.jdk8,
                    grpc.api,
                    "io.spine:protodata:${protoData.version}",
                    spine.reflect,
                    spine.base,
                    spine.time,
                    toolBase.lib,
                    toolBase.pluginBase,
                    logging.lib,
                    logging.libJvm,
                    logging.middleware,
                    io.spine.dependency.local.Validation.runtime,

                    // Temporarily force this dependencies during the migration to new versions.
                    "org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.7.10",
                    "org.jetbrains.kotlin:kotlin-project-model:1.7.10",
                    "org.jetbrains.kotlin:kotlin-tooling-core:1.7.10",
                    "org.jetbrains.kotlin:kotlin-gradle-plugin-model:1.7.10",
                    "org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10",
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0"
                )
            }
        }
    }
}

@Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
plugins {
    java
    idea
    id("com.google.protobuf")
    id("net.ltgt.errorprone")
}

val baseRoot = "$rootDir/.."

allprojects {
    apply(from = "$baseRoot/version.gradle.kts")
    apply(plugin = "java")

    repositories {
        standardToSpineSdk()
        gitHub("base")
        gitHub("tool-base")
        gitHub("model-compiler")
        mavenLocal()
    }

    group = "io.spine.tools.tests"
    version = extra["versionToPublish"]!!

    configurations {
        forceVersions()
        excludeProtobufLite()
        all {
            resolutionStrategy {
                force(
                    Kotlin.stdLibJdk7,
                    KotlinX.Coroutines.core,
                    KotlinX.Coroutines.jdk8,
                    Grpc.api,
                    Spine.reflect,
                    Spine.base,
                    Spine.time,
                    Spine.testlib,
                    Spine.toolBase,
                    Spine.pluginBase,
                    Logging.lib,
                    Logging.libJvm,
                    Logging.middleware,
                    ToolBase.psiJava,
                    ProtoData.api,
                    Validation.config,
                    Validation.runtime,
                )
            }

            // Exclude this stale artifact from all transitive dependencies.
            exclude("io.spine", "spine-validate")
        }
    }
    disableDocumentationTasks()
}

subprojects {

    apply {
        plugin("com.google.protobuf")
        plugin("kotlin")
        plugin("io.spine.mc-java")
        plugin("net.ltgt.errorprone")
        plugin("idea")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        tasks.withType<JavaCompile>().configureEach {
            configureJavac()
            configureErrorProne()
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
        setFreeCompilerArgs()
    }

    dependencies {
        errorprone(ErrorProne.core)
        errorproneJavac(ErrorProne.javacPlugin)
        ErrorProne.annotations.forEach { compileOnly(it) }
        implementation(Spine.base)
        implementation(Logging.lib)
        testImplementation(Spine.testlib)
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)
    }

    with(configurations) {
        doForceVersions(this)
        all {
            resolutionStrategy {
                force(
                    Validation.runtime,
                )
            }
        }
    }

    tasks.test {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }

        include("**/*Test.class")
        configureLogging()
    }

    disableDocumentationTasks()
}
