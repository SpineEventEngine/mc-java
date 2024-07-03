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

package io.spine.tools.mc.java.settings

import io.spine.annotation.Internal
import io.spine.protodata.CodegenContext
import io.spine.protodata.MessageType
import io.spine.protodata.renderer.MessageAction
import io.spine.protodata.renderer.SourceFile
import io.spine.reflect.Factory
import io.spine.tools.code.Java
import io.spine.tools.mc.java.settings.MessageActionFactory.Companion.EXIT_CODE
import io.spine.tools.mc.java.settings.MessageActionFactory.Companion.createAction
import kotlin.system.exitProcess
import org.checkerframework.checker.signature.qual.FqBinaryName

/**
 * The factory for creating instances of `MessageAction<Java>` by their class names.
 *
 * The factory terminates the process with the exit code specified
 * in the [EXIT_CODE] constant if an action could not be created.
 * The stacktrace of the exception is printed to [System.err].
 *
 * @see createAction
 */
@Internal
public class MessageActionFactory(classLoader: ClassLoader) :
    Factory<MessageAction<Java>>(classLoader) {

    private fun tryCreate(
        className: @FqBinaryName String,
        type: MessageType,
        file: SourceFile<Java>,
        context: CodegenContext
    ): MessageAction<Java> {
        @Suppress("TooGenericExceptionCaught") // Intentionally.
        val action = try {
            create(className, type, file, context)
        } catch (th: Throwable) {
            printError("Unable to create an instance of the class: `$className`.")
            printError("Please make sure that the class is available in the classpath.")
            printError(th.stackTraceToString())
            exitProcess(EXIT_CODE)
        }
        return action
    }

    private fun printError(str: String) {
        System.err.println(str)
    }

    public companion object {
        /**
         * The value for the exit code of the process in case of an error during action creation.
         */
        public const val EXIT_CODE: Int = 10

        /**
         * Creates an action with the given name and parameters.
         */
        public fun createAction(
            classLoader: ClassLoader,
            className: @FqBinaryName String,
            type: MessageType,
            file: SourceFile<Java>,
            context: CodegenContext
        ): MessageAction<Java> {
            val factory = MessageActionFactory(classLoader)
            val action = factory.tryCreate(className, type, file, context)
            return action
        }
    }
}
