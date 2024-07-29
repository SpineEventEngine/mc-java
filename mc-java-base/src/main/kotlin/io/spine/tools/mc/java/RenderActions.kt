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

package io.spine.tools.mc.java

import io.spine.protodata.CodegenContext
import io.spine.protodata.MessageType
import io.spine.protodata.renderer.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.mc.java.settings.MessageActionFactory.Companion.createAction
import org.checkerframework.checker.signature.qual.FqBinaryName

/**
 * Runs code generation actions for the given [type].
 *
 * @property type the message type for which code generation is performed.
 * @property file the file with the Java class with the message type.
 * @property actions fully qualified names of the action classes.
 * @property context the code generation context of the operation.
 *
 * @see ActionListRenderer
 * @see MessageTypeRenderer
 */
public class RenderActions(
    private val type: MessageType,
    private val file: SourceFile<Java>,
    private val actions: List<@FqBinaryName String>,
    private val context: CodegenContext
) {
    /**
     * Applies code generation to the [file].
     */
    public fun apply() {
        actions.forEach { actionClass ->
            runAction(actionClass)
        }
    }

    private fun runAction(actionClass: String) {
        val classloader = Thread.currentThread().contextClassLoader
        val action = createAction(
            classloader,
            actionClass,
            type,
            file,
            context
        )
        action.render()
    }
}
