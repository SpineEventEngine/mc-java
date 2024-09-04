/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.uuid

import com.google.protobuf.Message
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import io.spine.protobuf.pack
import io.spine.protodata.settings.actions
import io.spine.tools.mc.java.MessageAction
import io.spine.tools.mc.java.PluginTestSetup
import io.spine.tools.mc.java.comparable.ComparablePlugin
import io.spine.tools.mc.java.settings.Comparables
import io.spine.tools.mc.java.settings.Uuids
import io.spine.tools.mc.java.settings.comparables
import io.spine.tools.psi.java.topLevelClass
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * The base class for companion objects of test suites that test codegen
 * actions of [ComparablePlugin].
 */
abstract class ComparablePluginTestSetup(
    private val actionClass: Class<out MessageAction<*>>,
    private val parameter: Message,
) : PluginTestSetup<Comparables>(ComparablePlugin(), ComparablePlugin.SETTINGS_ID) {

    /**
     * Creates an instance of [Uuids] with only one action under the test.
     */
    override fun createSettings(projectDir: Path): Comparables = comparables {
        actions = actions {
            action.put(actionClass.name, parameter.pack())
        }
    }

    /**
     * Generates code for the given [message] simple name and returns the resulting
     * [PsiClass] along with the generated code (as plain text).
     */
    fun generateCode(message: String): Pair<PsiClass, String> {
        val projectDir = tempDir("projectDit")
        val outputDir = tempDir("outputDir")
        val settingsDir = tempDir("settingsDir")

        val sourceFileSet = runPipeline(projectDir, outputDir, settingsDir)
        val sourceFile = sourceFileSet.file(
            Path("io/spine/tools/mc/java/comparable/given/$message.java")
        )

        val file = sourceFile.psi() as PsiJavaFile
        val cls = file.topLevelClass
        val generatedCode = sourceFile.code()
        return cls to generatedCode
    }

    private fun tempDir(prefix: String): Path = Files.createTempDirectory(prefix)
}
