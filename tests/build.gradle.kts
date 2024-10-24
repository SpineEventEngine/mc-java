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

@file:Suppress("RemoveRedundantQualifierName") // To prevent IDEA replacing FQN imports.

import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Kotlin
import io.spine.internal.dependency.KotlinX
import io.spine.internal.dependency.ProtoData
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Truth
import io.spine.internal.dependency.Validation
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.publish.PublishingRepos.gitHub
import io.spine.internal.gradle.standardToSpineSdk
import io.spine.internal.gradle.testing.configureLogging
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {

    val baseRoot = "${rootDir}/.."
    val versionGradle = "${baseRoot}/version.gradle.kts"
    apply(from = versionGradle)

    standardSpineSdkRepositories()

    val mcJavaVersion: String by extra

    dependencies {
        classpath(io.spine.internal.dependency.Guava.lib)
        classpath(io.spine.internal.dependency.Protobuf.GradlePlugin.lib) {
            exclude(group = "com.google.guava")
        }
        classpath(io.spine.internal.dependency.ErrorProne.GradlePlugin.lib) {
            exclude(group = "com.google.guava")
        }
        classpath(io.spine.internal.dependency.ProtoData.pluginLib)
        classpath(io.spine.internal.dependency.Spine.McJava.pluginLib(mcJavaVersion))
    }

    with(configurations) {
        doForceVersions(this)
        val spine = io.spine.internal.dependency.Spine
        val logging = io.spine.internal.dependency.Spine.Logging
        val grpc = io.spine.internal.dependency.Grpc
        all {
            resolutionStrategy {
                force(
                    grpc.api,
                    "io.spine:protodata:${io.spine.internal.dependency.ProtoData.version}",
                    spine.reflect,
                    spine.base,
                    spine.time,
                    spine.toolBase,
                    spine.pluginBase,
                    logging.lib,
                    logging.libJvm,
                    logging.middleware,
                    io.spine.internal.dependency.Validation.runtime,
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
                    Spine.Logging.lib,
                    Spine.Logging.libJvm,
                    Spine.Logging.middleware,
                    Spine.psiJavaBundle,
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
        implementation(Spine.Logging.lib)
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

    idea.module {
        generatedSourceDirs.addAll(files(
                "$projectDir/generated/main/java",
                "$projectDir/generated/main/spine",
                "$projectDir/generated/test/java",
                "$projectDir/generated/test/spine"
        ))
    }

    sourceSets {
        main {
            proto.srcDir("$projectDir/src/main/proto")
            java.srcDirs("$projectDir/generated/main/java",
                         "$projectDir/generated/main/spine",
                         "$projectDir/generated/main/grpc",
                         "$projectDir/src/main/java")
            resources.srcDir("$projectDir/generated/main/resources")
        }

        test {
            proto.srcDir("$projectDir/src/test/proto")
            java.srcDirs("$projectDir/generated/test/java",
                         "$projectDir/generated/test/spine",
                         "$projectDir/generated/test/grpc",
                         "$projectDir/src/test/java")
            resources.srcDir("$projectDir/generated/test/resources")
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

    //TODO:2021-07-22:alexander.yevsyukov: Turn to WARN and investigate duplicates.
    // see https://github.com/SpineEventEngine/base/issues/657
    tasks.processTestResources.get().duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
