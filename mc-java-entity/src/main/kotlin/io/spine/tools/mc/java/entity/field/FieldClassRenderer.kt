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

package io.spine.tools.mc.java.entity.field

import io.spine.protodata.MessageType
import io.spine.protodata.java.ClassName
import io.spine.protodata.renderer.SourceFile
import io.spine.tools.mc.java.FieldClassFactory
import io.spine.tools.mc.java.codegen.GenerateFields
import io.spine.tools.mc.java.entity.EntityStateRenderer
import io.spine.tools.psi.java.execute
import io.spine.type.shortDebugString

/**
 * Renders classes named [Field][FieldClassFactory.CLASS_NAME] which are nested into
 * [EntityState][io.spine.base.EntityState] classes.
 *
 * @see io.spine.tools.mc.entity.DiscoveredEntitiesView
 * @see FieldClassFactory
 */
internal class FieldClassRenderer : EntityStateRenderer() {

    private val fieldSupertype: ClassName by lazy {
        val generateFields = settings.generateFields
        check(generateFields.hasSuperclass()) {
            val clsName = GenerateFields::class.java.canonicalName
            val debugStr = generateFields.shortDebugString()
            "Unable a field class supertype from this `$clsName` instance: `$debugStr`."
        }
        val cls = Class.forName(generateFields.superclass.canonical)
        ClassName(cls)
    }

    override fun doRender(type: MessageType, sourceFile: SourceFile) {
        execute {
            val factory = FieldClassFactory(type, fieldSupertype, typeSystem!!)
            factory.render(sourceFile)
        }
    }
}
