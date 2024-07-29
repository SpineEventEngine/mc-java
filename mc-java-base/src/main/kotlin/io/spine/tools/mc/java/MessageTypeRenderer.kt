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

import com.google.protobuf.Message
import io.spine.base.EntityState
import io.spine.protodata.File
import io.spine.protodata.MessageType
import io.spine.protodata.java.JavaRenderer
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.reflect.argumentIn
import io.spine.tools.code.Java

/**
 * An abstract base for Java renders handling message types.
 *
 * This class applies multiple render actions to multiple types.
 * For applying rendering actions to one type, please see [ActionListRenderer].
 *
 * @param V the type of the view state which gathers messages types served by this renderer.
 *  The type is an [EntityState] that has [File] as its identifier and
 *  implements the [io.spine.tools.mc.java.TypeListActions] interface.
 * @param S the type of the settings used by the renderer.
 *
 * @see ActionListRenderer
 */
//TODO:2024-07-29:alexander.yevsyukov: Rename to `MessageListRenderer`.
public abstract class MessageTypeRenderer<V, S : Message> :
    JavaRenderer() where V : EntityState<File>, V : TypeListActions {

    /**
     * The class matching by the generic parameter [V].
     */
    private val viewClass: Class<V> by lazy {
        genericArgument(0)
    }

    /**
     * The class matching the generic parameter [S].
     */
    private val settingsClass: Class<S> by lazy {
        genericArgument(1)
    }

    protected val settings: S by lazy {
        loadSettings(settingsClass)
    }

    /**
     * Tells if the [settings] allow this renderer to work.
     */
    protected abstract val enabledBySettings: Boolean

    /**
     * Implement this method to render the code for the given entity state [type]
     * the source code of which present in the given [file].
     */
    protected abstract fun doRender(type: MessageType, file: SourceFile<Java>)

    final override fun render(sources: SourceFileSet) {
        val relevant = sources.hasJavaRoot && enabledBySettings
        if (!relevant) {
            return
        }
        val types = findTypes()
        types.forEach {
            val sourceFile = sources.javaFileOf(it)
            doRender(it, sourceFile)
        }
    }

    /**
     * Finds message types declared in all proto files captured by the views.
     */
    private fun findTypes(): List<MessageType> {
        val found = select(viewClass).all()
        val result = found.flatMap { it.getTypeList() }
        return result
    }

    /**
     * Obtains a generic argument of a leaf class extending [MessageTypeRenderer].
     *
     * This way we don't have to pass information about the classes twice: as
     * generic type arguments and as classes passed to constructors.
     */
    private fun <T : Any> genericArgument(index: Int): Class<T> {
        @Suppress("UNCHECKED_CAST")
        return this::class.java.argumentIn<MessageTypeRenderer<V, S>>(index) as Class<T>
    }
}
