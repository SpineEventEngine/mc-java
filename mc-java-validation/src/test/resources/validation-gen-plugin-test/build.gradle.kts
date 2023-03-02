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

import io.spine.internal.gradle.applyStandardWithGitHub
import java.io.File
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool

// Common build file for the tests with same configuration

buildscript {

    // NOTE: this file is copied from the root project in the test setup.
    apply(from = "$rootDir/test-env.gradle")
    apply(from = "${extra["enclosingRootDir"]}/version.gradle.kts")

    io.spine.internal.gradle.applyWithStandard(this, rootProject,
        "base", "time", "change", "base-types", "core-java",
        "tool-base", "ProtoData", "validation",
    )

    val mcJavaVersion: String by extra
    dependencies {
        io.spine.internal.dependency.Protobuf.libs.forEach { classpath(it) }

        // Exclude `guava:18.0` as a transitive dependency by Protobuf Gradle plugin.
        classpath(io.spine.internal.dependency.Protobuf.GradlePlugin.lib) {
            exclude(group = "com.google.guava")
        }
        classpath("io.spine.tools:spine-mc-java-plugins:${mcJavaVersion}:all")
    }
}

plugins {
    java
}

apply {
    plugin("com.google.protobuf")
    plugin("io.spine.mc-java")
    from("$rootDir/test-env.gradle")
}

group = "io.spine.test"
version = "3.14"

repositories.applyStandardWithGitHub(project,
    "base", "time", "change", "base-types", "core-java",
    "tool-base", "ProtoData", "validation",
)

dependencies {
    implementation(io.spine.internal.dependency.Spine(project).base)
}

sourceSets {
    main {
        java.srcDirs("$projectDir/generated/main/java", "$projectDir/generated/main/spine")
        resources.srcDir("$projectDir/generated/main/resources")
        (extensions.getByName("proto") as SourceDirectorySet).srcDir("$projectDir/src/main/proto")
    }
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

fun File.residesIn(directory: File): Boolean =
    canonicalFile.startsWith(directory.absolutePath)
