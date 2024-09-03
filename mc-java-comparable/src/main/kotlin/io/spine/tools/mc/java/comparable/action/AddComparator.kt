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

package io.spine.tools.mc.java.comparable.action

import com.google.protobuf.Empty
import io.spine.protodata.CodegenContext
import io.spine.protodata.MessageType
import io.spine.protodata.ProtobufDependency
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.TypeName
import io.spine.protodata.renderer.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.mc.java.DirectMessageAction
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.comparable.ComparableActions
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory

/**
 * Updates the code of the message which qualifies as [Comparable] to
 * contain `compareTo()` method.
 *
 * The class is public because its fully qualified name is used as a default
 * value in [ComparableSettings][io.spine.tools.mc.java.gradle.settings.ComparableSettings].
 *
 * @property type the type of the message.
 * @property file the source code to which the action is applied.
 * @property context the code generation context in which this action runs.
 */
public class AddComparator(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : DirectMessageAction<Empty>(type, file, Empty.getDefaultInstance(), context) {

    private val validator = OptionValidator(::findMessage)
    private val builder = ComparatorBuilder()

    // TODO:2024-09-01:yevhenii.nadtochii: Can we ask a `TypeRenderer` pass it to us?
    //  This view contains a discovered `compare_by` option.
    private val option = select(ComparableActions::class.java)
        .findById(type)!!
        .option

    // TODO:2024-09-02:yevhenii.nadtochii: addFirst() and addLast() extensions are inconsistent.
    override fun doRender() {
        validator.check(option, type)
        val comparator = builder.composeAsText(cls.name!!, option)
        val field = elementFactory.createFieldFromText(comparator, cls)
        field.addFirst(GeneratedAnnotation.create())
        cls.addAfter(field, cls.lBrace)
    }

    private fun findMessage(typeName: TypeName): MessageType {
        val typeUrl = typeName.typeUrl
        val fromFiles = select(ProtobufSourceFile::class.java).all()
            .firstOrNull { it.containsType(typeUrl) }
            ?.typeMap?.get(typeUrl)

        if (fromFiles != null) {
            return fromFiles
        }

        val fromDependencies = select(ProtobufDependency::class.java).all()
            .firstOrNull { it.source.containsType(typeUrl) }
            ?.source?.typeMap?.get(typeUrl)
        return fromDependencies
            ?: error("`$typeUrl` not found in the passed Proto files and its dependencies.")
    }
}
