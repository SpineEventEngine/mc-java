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

package io.spine.tools.mc.annotation

import io.spine.protodata.plugin.Plugin
import io.spine.protodata.plugin.View
import io.spine.protodata.renderer.Renderer
import io.spine.server.BoundedContextBuilder
import io.spine.server.entity.Entity
import io.spine.tools.mc.java.annotation.EnumAnnotationRenderer
import io.spine.tools.mc.java.annotation.MessageAnnotationRenderer
import io.spine.tools.mc.java.annotation.ServiceAnnotationRenderer
import kotlin.reflect.KClass

/**
 * A ProtoData plugin which provides code generation for API level annotations.
 */
public class ApiAnnotationsPlugin : Plugin {

    override fun views(): Set<Class<out View<*, *, *>>> = setOf(
        AnnotatedMessageView::class.java,
        AnnotatedServiceView::class.java,
        AnnotatedEnumView::class.java
    )

    override fun renderers(): List<Renderer<*>> = listOf(
        MessageAnnotationRenderer(),
        ServiceAnnotationRenderer(),
        EnumAnnotationRenderer()
    )

    override fun extend(context: BoundedContextBuilder) {
        context.add(FileOptionsProcess::class)
    }
}

/**
 * Adds specified entity class to this `BoundedContextBuilder`.
 */
public inline fun <reified I, reified E : Entity<I, *>>
        BoundedContextBuilder.add(entity: KClass<out E>) {
    add(entity.java)
}