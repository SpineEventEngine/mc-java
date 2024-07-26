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

import io.kotest.matchers.shouldBe
import io.spine.base.UuidValue
import io.spine.tools.kotlin.reference
import io.spine.tools.mc.java.PluginTestSetup
import io.spine.tools.mc.java.settings.Uuids
import io.spine.tools.mc.java.settings.copy
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`ImplementUuidValue` should")
internal class ImplementUuidValueSpec {

    companion object : PluginTestSetup<Uuids>(UuidPlugin(), UuidPlugin.SETTINGS_ID) {

        lateinit var generatedCode: String

        /**
         * Creates an instance of [Uuids] with only one action under the test.
         */
        @JvmStatic
        override fun createSettings(projectDir: Path): Uuids {
            val codegenConfig = createCodegenConfig(projectDir)
            return codegenConfig.toProto().uuids.copy {
                action.clear()
                action.add(ImplementUuidValue::class.reference)
            }
        }

        @BeforeAll
        @JvmStatic
        fun setup(
            @TempDir projectDir: Path,
            @TempDir outputDir: Path,
            @TempDir settingsDir: Path
        ) {
            val sourceFileSet = runPipeline(projectDir, outputDir, settingsDir)
            val sourceFile = sourceFileSet.file(
                Path("io/spine/tools/mc/java/uuid/given/AccountId.java")
            )
            generatedCode = sourceFile.code()
        }
    }

    @Test
    fun `make a message implement 'UuidValue'`() {
        implementsInterface(generatedCode, UuidValue::class.reference) shouldBe true
    }
}

fun implementsInterface(javaCode: String, interfaceName: String): Boolean {
    val regex = Regex("""implements.*$interfaceName""", DOT_MATCHES_ALL)
    return regex.containsMatchIn(javaCode)
}
