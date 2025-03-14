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

package io.spine.tools.mc.java.ksp.gradle

import org.gradle.api.Project
import java.nio.file.Path

/**
 * The coordinates and utilities for working with Kotlin Symbol Processing Gradle Plugin.
 *
 * @see <a href="https://github.com/google/ksp">KSP GitHub repository</a>
 */
@Suppress("ConstPropertyName")
public object KspGradlePlugin {

    /**
     * The ID of the Gradle plugin.
     */
    public const val id: String = "com.google.devtools.ksp"

    /**
     * Obtains the full most recent version of KSP plugin compatible with
     * the given Kotlin version.
     */
    public fun findCompatible(kv: KotlinVersion): String {
        val mostRecentCompatible = versions.keys.findLast { kv >= it }
        checkNotNull(mostRecentCompatible) {
            "Unable to find the KSP plugin version compatible with Kotlin $kv."
        }
        val pluginVersion = versions[mostRecentCompatible]
        return "$mostRecentCompatible-$pluginVersion"
    }

    public fun defaultTargetDirectory(project: Project): Path {
        val generatedDir = project.layout.buildDirectory.dir("generated")
        return generatedDir.get().asFile.toPath()
    }

    /**
     * The Maven group of the KSP tools.
     */
    private const val group = "com.google.devtools.ksp"

    /**
     * The Maven reference to the plugin module
     */
    public const val module: String = "$group:symbol-processing-gradle-plugin"

    /**
     * Obtains Maven coordinates of the KSP Gradle Plugin with the given version.
     */
    public fun gradlePluginArtifact(version: String): String = "$module:$version"

    /**
     * The map from [KotlinVersion] to the latest KSP Gradle Plugin version which
     * supports this version of Kotlin.
     *
     * The map resembles the [combined version numbers](https://github.com/google/ksp/releases) of
     * the KSP Gradle Plugin for finding the best match for a Gradle project.
     *
     * Only release (not Beta or RC) versions are listed.
     */
    @Suppress("MagicNumber")
    private val versions: Map<KotlinVersion, String> = sortedMapOf(
        kv(1, 7, 0) to "1.0.6",
        kv(1, 7, 10) to "1.0.6",
        kv(1, 7, 20) to "1.0.6",
        kv(1, 7, 22) to "1.0.8",
        kv(1, 8, 22) to "1.0.11",
        kv(1, 9, 0) to "1.0.13",
        kv(1, 9, 10) to "1.0.13",
        kv(1, 9, 20) to "1.0.14",
        kv(1, 9, 21) to "1.0.16",
        kv(1, 9, 22) to "1.0.17",
        kv(1, 9, 23) to "1.0.19",
        kv(1, 9, 24) to "1.0.20",
        kv(1, 9, 25) to "1.0.20",
        kv(2, 0, 0) to "1.0.23",
        kv(2, 0, 10) to "1.0.24",
        kv(2, 0, 20) to "1.0.25",
        kv(2, 0, 21) to "1.0.28",
        kv(2, 1, 0) to "1.0.29",
        kv(2, 1, 10) to "1.0.30",
    )

    private fun kv(major: Int, minor: Int, patch: Int) = KotlinVersion(major, minor, patch)
}


