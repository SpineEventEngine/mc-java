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

import com.google.protobuf.Message
import io.spine.protodata.java.style.JavaCodeStyle
import io.spine.protodata.settings.Format
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.settings.defaultConsumerId
import io.spine.tools.mc.annotation.ApiAnnotationsPlugin
import io.spine.tools.mc.java.annotation.SettingsKt.annotationTypes
import io.spine.tools.mc.java.annotation.settings
import io.spine.tools.mc.java.entity.EntityPlugin
import io.spine.tools.mc.java.settings.CodegenOptions
import io.spine.tools.mc.java.settings.signalSettings
import io.spine.tools.mc.java.gradle.McJavaOptions
import io.spine.tools.mc.java.gradle.mcJava
import io.spine.tools.mc.java.gradle.plugins.WriteProtoDataSettings.Companion.ANNOTATION_SETTINGS_ID
import io.spine.tools.mc.java.gradle.plugins.WriteProtoDataSettings.Companion.ENTITY_SETTINGS_ID
import io.spine.tools.mc.java.gradle.plugins.WriteProtoDataSettings.Companion.JAVA_CODE_STYLE_ID
import io.spine.tools.mc.java.gradle.plugins.WriteProtoDataSettings.Companion.SIGNALS_SETTINGS_ID
import io.spine.tools.mc.java.gradle.plugins.WriteProtoDataSettings.Companion.VALIDATION_SETTINGS_ID
import io.spine.tools.mc.java.signal.SignalPlugin
import io.spine.type.toJson
import io.spine.validation.messageMarkers
import io.spine.validation.validationConfig
import java.io.IOException
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
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

    @get:Internal
    internal val options: McJavaOptions by lazy {
        project.mcJava
    }

    @TaskAction
    @Throws(IOException::class)
    private fun writeFile() {
        val settings = settingsDirectory()
        forValidationPlugin(settings)
        forAnnotationPlugin(settings)
        forEntityPlugin(settings)
        forSignalPlugin(settings)
        forStyleFormattingPlugin(settings)
    }

    internal companion object {

        /**
         * The ID used by Validation plugin components to load the settings.
         */
        const val VALIDATION_SETTINGS_ID = "io.spine.validation.ValidationPlugin"

        /**
         * The ID used by Annotation plugin components to load the settings.
         */
        val ANNOTATION_SETTINGS_ID: String = ApiAnnotationsPlugin::class.java.canonicalName

        /**
         * The ID used by Entity plugin components to load settings.
         */
        val ENTITY_SETTINGS_ID: String = EntityPlugin::class.java.canonicalName

        /**
         * The ID used by the Signals Plugin components to load settings.
         */
        val SIGNALS_SETTINGS_ID: String = SignalPlugin::class.java.canonicalName

        /**
         * The ID for the Java code style settings.
         */
        val JAVA_CODE_STYLE_ID = JavaCodeStyle::class.java.defaultConsumerId
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
private fun WriteProtoDataSettings.forValidationPlugin(settings: SettingsDirectory) {
    val codegen = options.codegen!!.toProto()
    val signalSettings = codegen.signalSettings
    val markers = messageMarkers {
        signalSettings.let {
            commandPattern.addAll(it.commands.patternList)
            eventPattern.addAll(it.events.patternList)
            rejectionPattern.addAll(it.rejections.patternList)
        }
        entityOptionName.addAll(codegen.entityOptionsNames())
    }
    val config = validationConfig {
        messageMarkers = markers
    }

    settings.write(VALIDATION_SETTINGS_ID, config)
}

private fun CodegenOptions.entityOptionsNames(): Iterable<String> =
    entities.optionList.map { it.name }

private fun WriteProtoDataSettings.forAnnotationPlugin(settings: SettingsDirectory) {
    val annotation = options.annotation
    val proto = settings {
        val javaType = annotation.types
        annotationTypes = annotationTypes {
            experimental = javaType.experimental.get()
            beta = javaType.beta.get()
            spi = javaType.spi.get()
            internal = javaType.internal.get()
        }
        internalClassPattern.addAll(annotation.internalClassPatterns.get())
        internalMethodName.addAll(annotation.internalMethodNames.get())
    }
    settings.write(ANNOTATION_SETTINGS_ID, proto)
}

private fun WriteProtoDataSettings.forEntityPlugin(settings: SettingsDirectory) {
    val entitySettings = options.codegen!!.entities().toProto()
    settings.write(ENTITY_SETTINGS_ID, entitySettings)
}

private fun WriteProtoDataSettings.forSignalPlugin(settings: SettingsDirectory) {
    val codegen = options.codegen!!
    val signalSettings = signalSettings {
        commands = codegen.commands().toProto()
        events = codegen.events().toProto()
        rejections = codegen.rejections().toProto()
    }
    settings.write(SIGNALS_SETTINGS_ID, signalSettings)
}

private fun WriteProtoDataSettings.forStyleFormattingPlugin(settings: SettingsDirectory) {
    val styleSettings = options.style.get()
    settings.write(JAVA_CODE_STYLE_ID, styleSettings)
}

/**
 * Writes the given instance of settings in [Format.PROTO_JSON] format using the [id].
 */
private fun SettingsDirectory.write(id: String, settings: Message) {
    write(id, Format.PROTO_JSON, settings.toJson())
}
