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

@file:Suppress("RemoveRedundantQualifierName") // To prevent IDEA replacing FQN imports.

import com.google.common.io.Files.createParentDirs
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import io.spine.internal.dependency.CheckerFramework
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.FindBugs
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.Jackson
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Truth
import io.spine.internal.gradle.publish.IncrementGuard
import io.spine.internal.gradle.RunBuild
import io.spine.internal.gradle.VersionWriter
import io.spine.internal.gradle.applyGitHubPackages
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.checkstyle.CheckStyleConfig
import io.spine.internal.gradle.excludeProtobufLite
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.publish.SpinePublishing
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.PublishingRepos.gitHub
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import java.time.Duration
import java.util.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    io.spine.internal.gradle.doApplyStandard(repositories)
}

plugins {
    `java-library`
    idea
    id(io.spine.internal.dependency.Protobuf.GradlePlugin.id)
    id(io.spine.internal.dependency.ErrorProne.GradlePlugin.id)
    kotlin("jvm")
    with(io.spine.internal.dependency.Spine.ProtoData) {
        id(pluginId) version version
    }
}

spinePublishing {
    modules = subprojects.map { it.name }.toSet()
    destinations = setOf(
        PublishingRepos.cloudRepo,
        PublishingRepos.cloudArtifactRegistry,
        gitHub("mc-java"),
    )
}

allprojects {
    apply {
        plugin("jacoco")
        plugin("idea")
        plugin("project-report")
        apply(from = "$rootDir/version.gradle.kts")
    }

    group = "io.spine.tools"
    version = extra["versionToPublish"]!!

    repositories {
        gitHub("base")
        gitHub("tool-base")
        gitHub("model-compiler")
        applyGitHubPackages("ProtoData", project)
        applyStandard()
    }
}

subprojects {
    apply {
        plugin("java-library")
        plugin("kotlin")
        plugin("net.ltgt.errorprone")
        plugin("pmd-settings")
        plugin(Protobuf.GradlePlugin.id)
        plugin("io.spine.protodata")
        plugin("maven-publish")
    }

    val validation = Spine(project).validation
    dependencies {
        errorprone(ErrorProne.core)

        protoData(validation.java)

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

    val baseVersion: String by extra
    val timeVersion: String by extra
    val toolBaseVersion: String by extra
    val serverVersion: String by extra
    val validationVersion: String by extra
    val protoDataVersion: String by extra
    configurations {
        forceVersions()
        excludeProtobufLite()
        all {
            // Exclude in favor of `spine-validation-java-runtime`.
            exclude("io.spine", "spine-validate")
            resolutionStrategy {
                force(
                    "io.spine:spine-base:$baseVersion",
                    "io.spine:spine-time:$timeVersion",
                    "io.spine:spine-server:$serverVersion",
                    "io.spine.tools:spine-testlib:$baseVersion",
                    "io.spine.tools:spine-tool-base:$toolBaseVersion",
                    "io.spine.tools:spine-plugin-base:$toolBaseVersion",
                    "io.spine.validation:spine-validation-java-runtime:$validationVersion",
                    "io.spine.protodata:protodata-codegen-java:$protoDataVersion",
                    "org.hamcrest:hamcrest-core:2.2",
                    Jackson.core,
                    Jackson.moduleKotlin,
                    Jackson.databind,
                    Jackson.bom,
                    Jackson.annotations,
                    Jackson.dataformatYaml
                )
            }
        }
    }

    java {
        tasks.withType<JavaCompile>().configureEach {
            configureJavac()
            configureErrorProne()
        }

        // Enforces the Java version for the output JARs
        // in case the project is built by JDK 12 or above.
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        explicitApi()

        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
            setFreeCompilerArgs()
        }
    }

    tasks {
        registerTestTasks()
        test {
            useJUnitPlatform {
                includeEngines("junit-jupiter")
            }
            configureLogging()
        }
    }

    val generatedDir = "$projectDir/generated"
    val generatedResources = "$generatedDir/main/resources"

    val prepareProtocConfigVersions by tasks.registering {
        description = "Prepares the versions.properties file."

        val propertiesFile = file("$generatedResources/versions.properties")
        outputs.file(propertiesFile)

        val versions = Properties().apply {
            setProperty("baseVersion", baseVersion)
            setProperty("protobufVersion", Protobuf.version)
            setProperty("gRPCVersion", Grpc.version)
        }

        @Suppress("UNCHECKED_CAST")
        inputs.properties(HashMap(versions) as MutableMap<String, *>)

        doLast {
            createParentDirs(propertiesFile)
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

    sourceSets.main {
        resources.srcDir(generatedResources)
    }

    apply<IncrementGuard>()
    apply<VersionWriter>()

    LicenseReporter.generateReportIn(project)
    JavadocConfig.applyTo(project)
    CheckStyleConfig.applyTo(project)

    protobuf { protoc { artifact = Protobuf.compiler } }

    protoData {
        renderers(
            "io.spine.validation.java.PrintValidationInsertionPoints",
            "io.spine.validation.java.JavaValidationRenderer",

            // Suppress warnings in the generated code.
            "io.spine.protodata.codegen.java.file.PrintBeforePrimaryDeclaration",
            "io.spine.protodata.codegen.java.suppress.SuppressRenderer"
        )
        plugins(
            "io.spine.validation.ValidationPlugin"
        )
    }

    project.configureTaskDependencies()
}

JacocoConfig.applyTo(project)
PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)

/**
 * Collect `publishToMavenLocal` tasks for all subprojects that are specified for
 * publishing in the root project.
 */
val publishedModules: Set<String> = extensions.getByType<SpinePublishing>().modules

val testAll by tasks.registering {
    val testTasks = publishedModules.map { p ->
        val subProject = project(p)
        subProject.tasks["test"]
    }
    dependsOn(testTasks)
}

val localPublish by tasks.registering {
    val pubTasks = publishedModules.map { p ->
        val subProject = project(p)
        subProject.tasks["publishToMavenLocal"]
    }
    dependsOn(pubTasks)
}

/**
 * The build task executed under `tests` subdirectory.
 *
 * These tests depend on locally published artifacts.
 * It is similar to the dependency on such artifacts that `:mc-java` module declares for
 * its tests. So, we depend on the `test` task of this module for simplicity.
 */
val integrationTests by tasks.registering(RunBuild::class) {
    directory = "$rootDir/tests"

    /** A timeout for the case of stalled child processes under Windows. */
    timeout.set(Duration.ofMinutes(20))
    dependsOn(testAll)
    dependsOn(localPublish)
}

tasks.register("buildAll") {
    dependsOn(tasks.build, integrationTests)
}

val check by tasks.existing {
    dependsOn(integrationTests)
}
