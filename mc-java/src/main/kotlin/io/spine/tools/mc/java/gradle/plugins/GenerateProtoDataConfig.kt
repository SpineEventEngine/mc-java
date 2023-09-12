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
package io.spine.tools.mc.java.gradle.plugins

import io.spine.protodata.gradle.plugin.LaunchProtoData
import io.spine.tools.mc.java.codegen.CodegenOptions
import io.spine.tools.mc.java.gradle.mcJava
import io.spine.tools.mc.java.gradle.plugins.GenerateProtoDataConfig.Companion.CONFIG_SUBDIR
import io.spine.validation.messageMarkers
import io.spine.validation.validationConfig
import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * A task that writes the ProtoData configuration into a file.
 *
 * The [targetFile] property defines the destination file.
 *
 * This task configures ProtoData-based validation codegen. It tells which files and types
 * are considered entities and signals, so that the Validation library may add extra constraints
 * for those types.
 */
@Suppress("unused") // Gradle creates a subtype for this class.
public abstract class GenerateProtoDataConfig : DefaultTask() {

    @get:OutputFile
    public abstract val targetFile: RegularFileProperty

    @TaskAction
    @Throws(IOException::class)
    private fun writeFile() {
        val options = project.mcJava
        val codegen = options.codegen.toProto()

        val makers = codegen.let {
            messageMarkers {
                commandPattern.addAll(it.commands.patternList)
                eventPattern.addAll(it.events.patternList)
                rejectionPattern.addAll(it.rejections.patternList)
                entityOptionName.addAll(it.entityOptionsNames())
            }
        }
        val config = validationConfig {
            messageMarkers = makers
        }

        val file = project.file(targetFile)
        file.parentFile.mkdirs()
        file.writeBytes(config.toByteArray())
    }

    internal companion object {

        internal fun taskNameFor(launchTask: LaunchProtoData): String =
            "writeConfigFor_${launchTask.name}"

        const val CONFIG_SUBDIR = "protodata-config"
    }
}

private fun CodegenOptions.entityOptionsNames(): Iterable<String> =
    entities.optionList.map { it.name }

internal fun GenerateProtoDataConfig.defaultFileName(): String {
    return CONFIG_SUBDIR + File.separatorChar + "$name.bin"
}
