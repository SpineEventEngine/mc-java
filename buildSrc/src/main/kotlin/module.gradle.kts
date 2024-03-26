/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import com.google.common.io.Files
import io.spine.internal.dependency.CheckerFramework
import io.spine.internal.dependency.Caffeine
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.FindBugs
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Jackson
import io.spine.internal.dependency.Kotlin
import io.spine.internal.dependency.KotlinX
import io.spine.internal.dependency.ProtoData
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Spine.Logging
import io.spine.internal.dependency.Truth
import io.spine.internal.dependency.Validation
import io.spine.internal.gradle.VersionWriter
import io.spine.internal.gradle.checkstyle.CheckStyleConfig
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.publish.IncrementGuard
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import java.util.*
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `java-library`
    kotlin("jvm")
    id("write-manifest")
    id("com.google.protobuf")
    id("net.ltgt.errorprone")
    id("pmd-settings")
    `maven-publish`
    id("detekt-code-analysis")
    id("project-report")
    idea
}

apply<IncrementGuard>()
apply<VersionWriter>()

LicenseReporter.generateReportIn(project)
JavadocConfig.applyTo(project)
CheckStyleConfig.applyTo(project)

private object BuildSettings {
    private const val JAVA_VERSION = 11
    val javaVersion: JavaLanguageVersion = JavaLanguageVersion.of(JAVA_VERSION)
}

project.run {
    addDependencies()
    forceConfigurations()

    val javaVersion = BuildSettings.javaVersion
    configureJava(javaVersion)
    configureKotlin(javaVersion)
    setupTests()

    val generatedDir = "$projectDir/generated"
    val generatedResources = "$generatedDir/main/resources"
    prepareProtocConfigVersionsTask(generatedResources)
    setupSourceSets(generatedResources)

    configureTaskDependencies()
}

typealias Module = Project

fun Module.addDependencies() {
    dependencies {
        errorprone(ErrorProne.core)

        compileOnlyApi(FindBugs.annotations)
        compileOnlyApi(CheckerFramework.annotations)
        ErrorProne.annotations.forEach { compileOnlyApi(it) }

        implementation(Guava.lib)
        implementation(Logging.lib)

        testImplementation(Guava.testLib)
        JUnit.api.forEach { testImplementation(it) }
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)

        testImplementation(Validation.runtime)
    }
}

fun Module.forceConfigurations() {
    configurations {
        forceVersions()
        excludeProtobufLite()
        all {
            // Exclude outdated module.
            exclude(group = "io.spine", module = "spine-logging-backend")

            // Exclude in favor of `spine-validation-java-runtime`.
            exclude("io.spine", "spine-validate")
            resolutionStrategy {
                @Suppress("DEPRECATION") // `Kotlin.stdLibJdk7` needs to be forced.
                force(
                    Kotlin.stdLibJdk7,
                    KotlinX.Coroutines.core,
                    KotlinX.Coroutines.jdk8,
                    Protobuf.compiler,
                    Grpc.api,
                    Grpc.core,
                    Grpc.protobuf,
                    Grpc.stub,
                    "org.yaml:snakeyaml:2.1",
                    Jackson.Junior.objects,
                    Caffeine.lib,

                    Spine.reflect,
                    Spine.base,
                    Spine.time,
                    Spine.server,
                    Spine.testlib,
                    Spine.toolBase,
                    Spine.pluginBase,
                    Spine.psiJava,
                    Logging.lib,

                    // Force the version to avoid the version conflict for
                    // the `:mc-java:ProtoData` configuration.
                    Validation.runtime,
                    validationJavaBundle(),
                    ProtoData.api,
                )
            }
        }
    }
}

fun Configuration.validationJavaBundle(): String {
    return if (name == "protoData") {
        Validation.javaBundle(Validation.dogfoodingVersion)
    } else {
        Validation.javaBundle
    }
}

fun Module.configureJava(javaVersion: JavaLanguageVersion) {
    tasks.withType<JavaCompile>().configureEach {
        val javaVer = javaVersion.toString()
        sourceCompatibility = javaVer
        targetCompatibility = javaVer
        configureJavac()
        configureErrorProne()
    }
}

fun Module.configureKotlin(javaVersion: JavaLanguageVersion) {
    kotlin {
        explicitApi()
    }
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = javaVersion.toString()
        setFreeCompilerArgs()
    }
}

fun Module.setupTests() {
    tasks {
        registerTestTasks()
        withType<Test> {
            useJUnitPlatform()
            configureLogging()
        }
    }
}

fun Module.prepareProtocConfigVersionsTask(generatedResources: String) {
    val prepareProtocConfigVersions by tasks.registering {
        description = "Prepares the versions.properties file."

        val propertiesFile = file("$generatedResources/versions.properties")
        outputs.file(propertiesFile)

        val versions = Properties().apply {
            setProperty("baseVersion", Spine.ArtifactVersion.base)
            setProperty("protobufVersion", Protobuf.version)
            setProperty("gRPCVersion", Grpc.version)
        }

        @Suppress("UNCHECKED_CAST")
        inputs.properties(HashMap(versions) as MutableMap<String, *>)

        doLast {
            Files.createParentDirs(propertiesFile)
            propertiesFile.createNewFile()
            propertiesFile.outputStream().use {
                versions.store(it,
                    "Versions of dependencies of the Spine Model Compiler for Java plugin and" +
                            " the Spine Protoc plugin.")
            }
        }
    }

    tasks.processResources {
        dependsOn(prepareProtocConfigVersions)
    }
}

fun Module.setupSourceSets(generatedResources: String) {
    sourceSets.main {
        resources.srcDir(generatedResources)
    }
}
