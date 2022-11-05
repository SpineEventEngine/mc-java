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
import io.spine.internal.gradle.applyStandardWithGitHub
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

    val spine = io.spine.internal.dependency.Spine(project)
    io.spine.internal.gradle.doForceVersions(configurations)
    configurations {
        all {
            resolutionStrategy {
                force(
                    spine.server,
                    spine.validation.java,
                    io.spine.internal.dependency.Spine.ProtoData.pluginLib
                )
            }
        }
    }
}

plugins {
    `java-library`
    idea
    kotlin("jvm")
    errorprone
    protobuf
    `gradle-doctor`
    id(protoData.pluginId) version protoData.version

}

spinePublishing {
    modules = subprojects.map { it.name }.toSet()
    modulesWithCustomPublishing = setOf(
        "mc-java-plugin-bundle"
    )
    destinations = PublishingRepos.run {
        setOf(
            cloudRepo,
            cloudArtifactRegistry,
            gitHub("mc-java"),
        )
    }
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

    repositories.applyStandardWithGitHub(project,
        "base", "tool-base", "model-compiler", "ProtoData", "validation"
    )
}

subprojects {
    applyPlugins()
    addDependencies()
    forceConfigurations()

    val javaVersion = JavaLanguageVersion.of(11)
    configureJava(javaVersion)
    configureKotlin(javaVersion)
    setupTests()

    setupCodegen()

    val generatedDir = "$projectDir/generated"
    val generatedResources = "$generatedDir/main/resources"
    prepareProtocConfigVersionsTask(generatedResources)
    setupSourceSets(generatedResources)

    configureTaskDependencies()
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

typealias Subproject = Project

fun Subproject.applyPlugins() {
    apply {
        plugin("java-library")
        plugin("kotlin")
        plugin("net.ltgt.errorprone")
        plugin("pmd-settings")
        plugin(Protobuf.GradlePlugin.id)
        plugin("io.spine.protodata")
        plugin("maven-publish")
        plugin("io.spine.protodata")
        plugin("detekt-code-analysis")
    }

    apply<IncrementGuard>()
    apply<VersionWriter>()

    LicenseReporter.generateReportIn(project)
    JavadocConfig.applyTo(project)
    CheckStyleConfig.applyTo(project)
}

fun Subproject.addDependencies(): Spine {
    val spine = Spine(project)
    val validation = spine.validation
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
    return spine
}

fun Subproject.forceConfigurations() {
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

                    spine.validation.java,
                    // Force the version to avoid the version conflict for
                    // the `:mc-java:ProtoData` configuration.
                    spine.validation.runtime,
                    "io.spine.protodata:protodata-codegen-java:${Spine.protoDataVersion}",

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
}

fun Subproject.configureJava(javaVersion: JavaLanguageVersion) {
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        configureJavac()
        configureErrorProne()
    }
}

fun Subproject.configureKotlin(javaVersion: JavaLanguageVersion) {
    kotlin {
        explicitApi()
    }
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = javaVersion.toString()
        setFreeCompilerArgs()
    }
}

fun Subproject.setupTests() {
    tasks {
        registerTestTasks()
        test {
            useJUnitPlatform {
                includeEngines("junit-jupiter")
            }
            configureLogging()
        }
    }
}

fun Subproject.prepareProtocConfigVersionsTask(generatedResources: String) {
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
}

fun Subproject.setupCodegen() {

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
}

fun Project.setupSourceSets(generatedResources: String) {
    sourceSets.main {
        resources.srcDir(generatedResources)
    }
}
