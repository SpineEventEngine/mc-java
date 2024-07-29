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

import io.spine.base.EntityState
import io.spine.protodata.CodegenContext
import io.spine.protodata.MessageType
import io.spine.protodata.java.JavaRenderer
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.reflect.argumentIn
import io.spine.tools.code.Java
import io.spine.tools.mc.java.settings.MessageActionFactory.Companion.createAction
import io.spine.tools.psi.java.execute
import org.checkerframework.checker.signature.qual.FqBinaryName

/**
 * The abstract base for renderers running one or more render actions on a message type.
 *
 * The type and actions are obtained from a view implementing [WithActionList].
 * The renderer acts on all the views queried by their [viewClass].
 */
public abstract class ActionListRenderer<V>  : JavaRenderer()
    where V: EntityState<*>, V: WithActionList {

    private val viewClass: Class<V> by lazy {
        @Suppress("UNCHECKED_CAST")
        this::class.java.argumentIn<ActionListRenderer<V>>(0) as Class<V>
    }

    override fun render(sources: SourceFileSet) {
        val relevant = sources.hasJavaRoot
        if (!relevant) {
            return
        }
        val views = findViews()
        views.forEach { view ->
            val type = view.getType()
            val sourceFile = sources.javaFileOf(type)
            execute {
                doRender(view, type, sourceFile)
            }
        }
    }

    private fun doRender(view: V, type: MessageType, sourceFile: SourceFile<Java>) {
        val actionList = view.getActionList()
        val actions = RenderActions(type, sourceFile, actionList, context!!)
        actions.apply()
    }

    private fun findViews(): Set<V> {
        val found = select(viewClass).all()
        return found
    }
}

/**
 * Runs code generation actions for the given [type].
 *
 * @property type the message type for which code generation is performed.
 * @property file the file with the Java class with the message type.
 * @property actions fully qualified names of the action classes.
 * @property context the code generation context of the operation.
 *
 * @see ActionListRenderer
 * @see
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
