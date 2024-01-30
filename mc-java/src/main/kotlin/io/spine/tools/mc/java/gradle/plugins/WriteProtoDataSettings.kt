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
package io.spine.tools.mc.java.gradle.plugins

import io.spine.protodata.settings.Format
import io.spine.protodata.settings.SettingsDirectory
import io.spine.tools.mc.java.annotation.SettingsKt.annotationTypes
import io.spine.tools.mc.java.annotation.settings
import io.spine.tools.mc.java.codegen.CodegenOptions
import io.spine.tools.mc.java.gradle.mcJava
import io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigPlugin.Companion.ANNOTATION_SETTINGS_ID
import io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigPlugin.Companion.VALIDATION_SETTINGS_ID
import io.spine.type.toJson
import io.spine.validation.messageMarkers
import io.spine.validation.validationConfig
import java.io.IOException
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * A task that writes settings for ProtoData.
 *
 * The [settingsDir] property defines the directory where settings files for
 * ProtoData plugins are stored.
 *
 * This task writes settings files for ProtoData components.
 */
@Suppress("unused") // Gradle creates a subtype for this class.
public abstract class WriteProtoDataSettings : DefaultTask() {

    @get:OutputDirectory
    public abstract val settingsDir: DirectoryProperty

    @TaskAction
    @Throws(IOException::class)
    private fun writeFile() {
        val settings = settingsDirectory()
        forValidation(settings)
        forAnnotation(settings)
    }
}

/**
 * Obtains an instance of [SettingsDirectory] to be used for writing files which
 * points to the directory specified by the [WriteProtoDataSettings.settingsDir] property.
 */
private fun WriteProtoDataSettings.settingsDirectory(): SettingsDirectory {
    val dir = project.file(settingsDir)
    dir.mkdirs()
    val settings = SettingsDirectory(dir.toPath())
    return settings
}

/**
 * Writes settings for Validation codegen.
 *
 * The settings are taken from McJava extension object and converted to
 * [io.spine.validation.ValidationConfig], which is later written as JSON file.
 */
private fun WriteProtoDataSettings.forValidation(settings: SettingsDirectory) {
    val options = project.mcJava
    val codegen = options.codegen!!.toProto()
    val markers = codegen.let {
        messageMarkers {
            commandPattern.addAll(it.commands.patternList)
            eventPattern.addAll(it.events.patternList)
            rejectionPattern.addAll(it.rejections.patternList)
            entityOptionName.addAll(it.entityOptionsNames())
        }
    }
    val config = validationConfig {
        messageMarkers = markers
    }

    settings.write(VALIDATION_SETTINGS_ID, Format.PROTO_JSON, config.toJson())
}

private fun CodegenOptions.entityOptionsNames(): Iterable<String> =
    entities.optionList.map { it.name }

private fun WriteProtoDataSettings.forAnnotation(settings: SettingsDirectory) {
    val options = project.mcJava
    val javaType = options.annotation.types
    val proto = settings {
        annotationTypes = annotationTypes {
            experimental = javaType.experimental.get()
            beta = javaType.beta.get()
            spi = javaType.spi.get()
            internal = javaType.internal.get()
        }
    }
    settings.write(ANNOTATION_SETTINGS_ID, Format.PROTO_JSON, proto.toJson())
}
