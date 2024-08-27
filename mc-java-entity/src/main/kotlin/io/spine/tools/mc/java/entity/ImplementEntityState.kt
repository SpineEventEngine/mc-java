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

package io.spine.tools.mc.java.entity

import com.google.protobuf.Empty
import io.spine.base.EntityState
import io.spine.protodata.CodegenContext
import io.spine.protodata.MessageType
import io.spine.protodata.java.javaType
import io.spine.protodata.qualifiedName
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.type.TypeSystem
import io.spine.tools.code.Java
import io.spine.tools.java.reference
import io.spine.tools.mc.java.DirectMessageAction
import io.spine.tools.mc.java.ImplementInterface

/**
 * Updates the Java code of a message type which qualifies as [EntityState] by
 * making it implement this interface.
 *
 * The class is public because its fully qualified name is used as a default
 * value in [UuidSettings][io.spine.tools.mc.java.gradle.settings.EntitySettings].
 *
 * @property type the type of the message.
 * @property file the source code to which the action is applied.
 * @property context the code generation context in which this action runs.
 */
public class ImplementEntityState(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : DirectMessageAction<Empty>(type, file, Empty.getDefaultInstance(), context) {

    override fun doRender() {
        val idFieldType = type.firstFieldType(typeSystem!!)
        val action = ImplementInterface(
            type,
            file,
            EntityState::class.java.reference,
            listOf(idFieldType),
            context!!
        )
        action.render()
    }
}

private fun MessageType.firstFieldType(typeSystem: TypeSystem): String {
    //TODO:2024-07-31:alexander.yevsyukov: Migrate to `MessageType.firstField` from ProtoData.
    check(fieldCount != 0) {
        "The type `${name.qualifiedName}` must have at least one field."
    }
    val field = getField(0)
    return field.javaType(typeSystem)
}
