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

import com.google.common.io.Files
import io.spine.internal.dependency.CheckerFramework
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.FindBugs
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Jackson
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Truth
import io.spine.internal.gradle.VersionWriter
import io.spine.internal.gradle.checkstyle.CheckStyleConfig
import io.spine.internal.gradle.excludeProtobufLite
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.publish.IncrementGuard
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import java.util.*
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.idea
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.`maven-publish`
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `java-library`
    kotlin("jvm")
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

    val javaVersion = JavaLanguageVersion.of(JAVA_VERSION)
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
    val validation = Spine(project).validation
    dependencies {
        errorprone(ErrorProne.core)

        compileOnlyApi(FindBugs.annotations)
        compileOnlyApi(CheckerFramework.annotations)
        ErrorProne.annotations.forEach { compileOnlyApi(it) }

        implementation(Guava.lib)

        testImplementation(Guava.testLib)
        JUnit.api.forEach { testImplementation(it) }
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)

        testImplementation(validation.runtime)
    }
}

fun Module.forceConfigurations() {
    val spine = Spine(project)
    configurations {
        forceVersions()
        excludeProtobufLite()
        all {
            // Exclude in favor of `spine-validation-java-runtime`.
            exclude("io.spine", "spine-validate")
            resolutionStrategy {
                force(
                    Protobuf.compiler,
                    spine.base,
                    spine.time,
                    spine.server,
                    spine.testlib,
                    spine.toolBase,
                    spine.pluginBase,

                    // Force the version to avoid the version conflict for
                    // the `:mc-java:ProtoData` configuration.
                    spine.validation.runtime,
                    "io.spine.protodata:protodata-codegen-java:${Spine.protoDataVersion}",

                    JUnit.runner,
                    "org.hamcrest:hamcrest-core:2.2",
                    Jackson.core,
                    Jackson.moduleKotlin,
                    Jackson.databind,
                    Jackson.bom,
                    Jackson.annotations,
                    Jackson.dataformatYaml,

                    // Transitive dependency.
                    "io.github.java-diff-utils:java-diff-utils:4.12"
                )
            }
        }
    }
}

fun Module.configureJava(javaVersion: JavaLanguageVersion) {
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
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
            setProperty("baseVersion", Spine.DefaultVersion.base)
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
