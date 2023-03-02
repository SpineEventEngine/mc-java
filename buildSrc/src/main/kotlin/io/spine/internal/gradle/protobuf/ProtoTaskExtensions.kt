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

package io.spine.internal.gradle.protobuf

import com.google.protobuf.gradle.GenerateProtoTask
import java.io.File
import java.lang.System.lineSeparator
import org.gradle.api.Task
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.get

/**
 * Configures protobuf code generation task for the code which cannot use Spine Model Compiler
 * (e.g. the `base` project).
 *
 * The task configuration consists of the following steps:
 *
 * 1. Adding `"kotlin"` to the list of involved `protoc` plugins.
 *
 * 2. Generation of descriptor set file is turned on for each source set.
 *    These files are placed under the `build/descriptors` directory.
 *
 * 3. Removing source code generated for `com.google` package for both Java and Kotlin.
 *    This is done at the final steps of the code generation.
 *
 * 4. Adding suppression of deprecation warnings in the generated Kotlin code.
 *
 * 5. Making `processResource` tasks depend on corresponding `generateProto` tasks.
 *    If the source set of the configured task isn't `main`, appropriate infix for
 *    the task names is used.
 *
 * The usage of this extension in a <em>module build file</em> would be:
 * ```
 *  val generatedDir by extra("$projectDir/generated")
 *  protobuf {
 *      generateProtoTasks {
 *         for (task in all()) {
 *            task.setup(generatedDir)
 *         }
 *     }
 * }
 * ```
 * Using the same code under `subprojects` in a root build file does not seem to work because
 * test descriptor set files are not copied to resources. Performing this configuration from
 * subprojects solves the issue.
 *
 * IMPORTANT: In addition to calling `setup`, a submodule must contain a descriptor set reference
 * file (`desc.ref`) files placed under `resources`. The descriptor reference file must contain
 * a reference to the descriptor set file generated by the corresponding `GenerateProtoTask`.
 *
 * For example, for the `test` source set, the reference would be `known_types_test.desc`, and
 * for the `main` source set, the reference would be `known_types_main.desc`.
 *
 * See `io.spine.code.proto.DescriptorReference` and `io.spine.code.proto.FileDescriptors` classes
 * under the `base` project for more details.
 */
@Suppress("unused")
fun GenerateProtoTask.setup(generatedDir: String) {

    builtins.maybeCreate("kotlin")

    /**
     * Generate descriptor set files.
     */
    val ssn = sourceSet.name
    generateDescriptorSet = true
    with(descriptorSetOptions) {
        path = "${project.buildDir}/descriptors/${ssn}/known_types_${ssn}.desc"
        includeImports = true
        includeSourceInfo = true
    }

    doLast {
        deleteComGoogle(generatedDir, ssn, "java")
        deleteComGoogle(generatedDir, ssn, "kotlin")
    }

    /**
     * Make the tasks `processResources` depend on `generateProto` tasks explicitly so that:
     *  1) descriptor set files get into resources, avoiding the racing conditions
     *     during the build.
     *  2) we don't have the warning "Execution optimizations have been disabled..." issued
     *     by Gradle during the build because Protobuf Gradle Plugin does not set
     *     dependencies between `generateProto` and `processResources` tasks.
     */
    val processResources = processResourceTaskName(ssn)
    project.tasks[processResources].dependsOn(this)
}

/**
 * Remove the code generated for Google Protobuf library types.
 *
 * Java code for the `com.google` package was generated because we wanted
 * to have descriptors for all the types, including those from Google Protobuf library.
 * We want all the descriptors so that they are included into the resources used by
 * the `io.spine.type.KnownTypes` class.
 *
 * Now, as we have the descriptors _and_ excessive Java or Kotlin code, we delete it to avoid
 * classes that duplicate those coming from Protobuf library JARs.
 */
private fun Task.deleteComGoogle(generatedDir: String, ssn: String, language: String) {
    val comDirectory = File("${generatedDir}/${ssn}/$language/com")
    val googlePackage = comDirectory.resolve("google")
    project.delete(googlePackage)

    // If the `com` directory becomes empty, delete it too.
    if (comDirectory.exists() && comDirectory.isDirectory && comDirectory.list()!!.isEmpty()) {
        project.delete(comDirectory)
    }
}

/**
 * Obtains the name of the task `processResource` task for the given source set name.
 */
private fun processResourceTaskName(sourceSetName: String): String {
    val infix = if (sourceSetName == "main") "" else sourceSetName.capitalized()
    return "process${infix}Resources"
}

/**
 * Obtains the path to this source code file, starting from `buildSrc`.
 */
private object SourcePath {

    val value: String
        get() {
            val thisClass = SourcePath::class.java
            val filePath  = thisClass.`package`.name.replace('.', '/') + "/ProtoTaskExtensions.kt"
            return "buildSrc/src/main/kotlin/$filePath"
        }
}
