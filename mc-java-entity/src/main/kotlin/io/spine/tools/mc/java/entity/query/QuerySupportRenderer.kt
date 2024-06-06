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

package io.spine.tools.mc.java.entity.query

import io.spine.protodata.MessageType
import io.spine.protodata.renderer.SourceFile
import io.spine.tools.mc.java.entity.EntityStateRenderer
import io.spine.tools.psi.java.execute

/**
 * Renders [Query][QueryClass], [QueryBuilder][QueryBuilderClass] classes and
 * the [query][QueryMethod] under a Java class generated for an entity state type.
 */
internal class QuerySupportRenderer : EntityStateRenderer() {

    override fun doRender(type: MessageType, sourceFile: SourceFile) {
        execute {
            val ts = typeSystem!!
            val queryBuilderClass = QueryBuilderClass(type, ts, settings)
            queryBuilderClass.render(sourceFile)
            val queryClass = QueryClass(type, ts, settings)
            queryClass.render(sourceFile)
            val queryMethod = QueryMethod(sourceFile)
            queryMethod.render()
        }
    }
}