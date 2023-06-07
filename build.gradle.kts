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

@file:Suppress("RemoveRedundantQualifierName") // To prevent IDEA replacing FQN imports.

import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.RunBuild
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.SpinePublishing
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import io.spine.internal.gradle.standardToSpineSdk
import java.time.Duration
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool

buildscript {
    standardSpineSdkRepositories()

    val spine = io.spine.internal.dependency.Spine
    io.spine.internal.gradle.doForceVersions(configurations)
    configurations {
        all {
            resolutionStrategy {
                force(
                    spine.base,
                    spine.toolBase,
                    spine.server,
                    spine.logging,
                    io.spine.internal.dependency.Validation.runtime,
                    io.spine.internal.dependency.ProtoData.pluginLib
                )
            }
        }
    }
}

plugins {
    idea
    errorprone
    jacoco
    `gradle-doctor`
    id("project-report")
    protobuf
    java
    id(protoData.pluginId) version protoData.version
}

private object BuildSettings {
    const val TIMEOUT_MINUTES = 20L
}

spinePublishing {
    modules = subprojects.map { it.name }.toSet()
    destinations = PublishingRepos.run {
        setOf(
            cloudRepo,
            cloudArtifactRegistry,
            gitHub("mc-java"),
        )
    }
}

allprojects {
    apply(from = "$rootDir/version.gradle.kts")
    group = "io.spine.tools"
    version = extra["versionToPublish"]!!
    repositories.standardToSpineSdk()
}

subprojects {
    apply(plugin = "module")
    apply(plugin = "io.spine.protodata")
    dependencies {
        protoData(Spine.validation.java)
    }
    setupCodegen()
}

JacocoConfig.applyTo(project)
PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)

/**
 * Collect `publishToMavenLocal` tasks for all subprojects that are specified for
 * publishing in the root project.
 */
val publishedModules: Set<String> = extensions.getByType<SpinePublishing>().modules

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
    timeout.set(Duration.ofMinutes(BuildSettings.TIMEOUT_MINUTES))

    /** Run integration tests only after all regular tests pass in all modules. */
    subprojects.forEach {
        it.tasks.findByName("test")?.let { testTask ->
            this@registering.dependsOn(testTask)
        }
    }
    dependsOn(localPublish)
    doLast {
        val f = file("$directory/_out/error-out.txt")
        project.logger.error(f.readText())
    }
}

tasks.register("buildAll") {
    dependsOn(integrationTests)
}

val check by tasks.existing {
    dependsOn(integrationTests)
}

typealias Module = Project

/**
 * We configure ProtoData at the project level (and not in the `module.gradle.kts` script plugin)
 * because here the Gradle plugin is already applied and its extension is available.
 */
fun Module.setupCodegen() {

    protobuf {
        //generatedFilesBaseDir = "$projectDir/generated"
        protoc { artifact = Protobuf.compiler }
    }

    protoData {
        renderers(
            "io.spine.validation.java.PrintValidationInsertionPoints",
            "io.spine.validation.java.JavaValidationRenderer",

            // Suppress warnings in the generated code.
            "io.spine.protodata.codegen.java.file.PrintBeforePrimaryDeclaration",
            "io.spine.protodata.codegen.java.annotation.SuppressWarningsAnnotation"
        )
        plugins(
            "io.spine.validation.ValidationPlugin"
        )
    }

    val generatedSourceProto = "$buildDir/generated/source/proto"

    /**
     * Remove the generated vanilla proto code.
     */
    project.afterEvaluate {
        val generatedSourceProtoDir = File(generatedSourceProto)
        val notInSourceDir: (File) -> Boolean = { file -> !file.residesIn(generatedSourceProtoDir) }

        tasks.withType<JavaCompile>().forEach {
            it.source = it.source.filter(notInSourceDir).asFileTree
        }

        tasks.withType<KotlinCompile<*>>().forEach {
            val thisTask = it as KotlinCompileTool
            val filteredKotlin = thisTask.sources.filter(notInSourceDir).toSet()
            with(thisTask.sources as ConfigurableFileCollection) {
                setFrom(filteredKotlin)
            }
        }
    }
}

fun File.residesIn(directory: File): Boolean =
    canonicalFile.startsWith(directory.absolutePath)
