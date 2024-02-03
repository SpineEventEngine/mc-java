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

package io.spine.tools.mc.java

import com.intellij.core.CoreApplicationEnvironment.registerApplicationDynamicExtensionPoint
import com.intellij.mock.MockProject
import com.intellij.openapi.command.impl.CoreCommandProcessor
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl
import com.intellij.pom.PomModel
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.impl.source.tree.TreeCopyHandler
import io.spine.protodata.codegen.java.file.PsiJavaParser

internal object PsiWriteSupport {

    private val lock = Object()
    private var initialized = false

    private val project: MockProject by lazy {
        PsiJavaParser.instance.project as MockProject
    }

    private val extensionArea: ExtensionsArea by lazy {
        val result = ExtensionsAreaImpl(project)
        Extensions.setRootArea(result)
        result
    }

    private val commandProcessor: CoreCommandProcessor by lazy {
        if (!initialized) {
            init()
        }
        CoreCommandProcessor()
    }

    fun execute(runnable: Runnable) {
        commandProcessor.executeCommand(project, runnable, null, null)
    }

    internal fun init() {
        if (initialized) {
            return
        }

        // https://github.com/ansman/kotlin/blob/e4b574de3893a2f8bf9c6aa2c975b85f91cae61d/native/native.tests/tests/org/jetbrains/kotlin/konan/test/blackbox/support/group/ExtTestCaseGroupProvider.kt#L872
        synchronized(lock) {
            extensionArea // Force initialization of `ExtensionArea` static.

            registerApplicationDynamicExtensionPoint(
                TreeCopyHandler.EP_NAME.name,
                TreeCopyHandler::class.java
            )
            project.run {
                registerService(PomModel::class.java, PomModelImpl::class.java)
                registerService(TreeAspect::class.java)
            }
        }
        initialized = true
    }
}
