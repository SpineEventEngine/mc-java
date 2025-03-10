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

package io.spine.tools.mc.java.routing.gradle

import io.kotest.matchers.shouldNotBe
import io.spine.testing.SlowTest
import io.spine.tools.gradle.project.sourceSets
import java.io.File
import java.net.URI
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.problems.Problem
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.ProblemReporter
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.internal.AdditionalDataBuilderFactory
import org.gradle.api.problems.internal.InternalProblem
import org.gradle.api.problems.internal.InternalProblemBuilder
import org.gradle.api.problems.internal.InternalProblemReporter
import org.gradle.api.problems.internal.InternalProblemSpec
import org.gradle.api.problems.internal.InternalProblems
import org.gradle.api.problems.internal.ProblemsProgressEventEmitterHolder
import org.gradle.internal.operations.OperationIdentifier
import org.gradle.internal.reflect.Instantiator
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@SlowTest
internal class RoutingPluginTest {

    companion object {

        private lateinit var project: Project

        @BeforeAll
        @JvmStatic
        fun setupProject(@TempDir projectDir: File) {
            // See: https://github.com/gradle/gradle/issues/31862#issuecomment-2687633265
            // and stub classes below.
            ProblemsProgressEventEmitterHolder.init(InternalProblemsStub())

            project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()

            project.buildscript.repositories.applyStandard()

            project.pluginManager.run {
                apply("java")
                apply("org.jetbrains.kotlin.jvm")
                apply(RoutingPlugin::class.java)
            }

            project.sourceSets.run {
                create("integrationTest")
            }

            // Force evaluation of the project.
            project.evaluationDependsOn(":")
        }
    }

    @Test
    fun `KSP plugin is applied`() {
        project.plugins.findPlugin(KspGradlePlugin.id) shouldNotBe null
    }
}

fun RepositoryHandler.applyStandard() {
    mavenLocal()
    mavenCentral()
    val registryBaseUrl = "https://europe-maven.pkg.dev/spine-event-engine"
    maven {
        it.url = URI("$registryBaseUrl/releases")
    }
    maven {
        it.url = URI("$registryBaseUrl/snapshots")
    }
}

////////////// Stubs copied from ToolBase `PlugableProjectSpec.kt`

/**
 * The stub class for workaround for
 * [this Gradle issue](https://github.com/gradle/gradle/issues/31862).
 *
 * @see <a href="https://github.com/gradle/gradle/issues/31862#issuecomment-2687633265">
 *     Workaround</a>
 */
private class InternalProblemsStub : InternalProblems {
    override fun getReporter(): ProblemReporter = notImplemented()
    override fun getInternalReporter(): InternalProblemReporter = InternalProblemReporterStub()
    override fun getAdditionalDataBuilderFactory(): AdditionalDataBuilderFactory = notImplemented()
    override fun getInstantiator(): Instantiator = notImplemented()
    override fun getProblemBuilder(): InternalProblemBuilder = notImplemented()
}

private fun notImplemented(): Nothing = TODO("Not yet implemented")

/**
 * The stub class for workaround for
 * [this Gradle issue](https://github.com/gradle/gradle/issues/31862).
 *
 * @see <a href="https://github.com/gradle/gradle/issues/31862#issuecomment-2687633265">
 *     Workaround</a>
 */
private class InternalProblemReporterStub : InternalProblemReporter {
    override fun create(problemId: ProblemId, action: Action<in ProblemSpec>): Problem =
        notImplemented()
    override fun report(problem: Problem, id: OperationIdentifier) = notImplemented()
    override fun report(problemId: ProblemId, spec: Action<in ProblemSpec>) = notImplemented()
    override fun report(problem: Problem) = notImplemented()
    override fun report(problems: MutableCollection<out Problem>) = notImplemented()

    override fun throwing(
        exception: Throwable,
        problemId: ProblemId,
        spec: Action<in ProblemSpec>
    ): RuntimeException = notImplemented()

    override fun throwing(exception: Throwable, problem: Problem): RuntimeException =
        notImplemented()

    override fun throwing(
        exception: Throwable,
        problems: MutableCollection<out Problem>
    ): RuntimeException = notImplemented()

    override fun internalCreate(action: Action<in InternalProblemSpec>): InternalProblem =
        notImplemented()
}
