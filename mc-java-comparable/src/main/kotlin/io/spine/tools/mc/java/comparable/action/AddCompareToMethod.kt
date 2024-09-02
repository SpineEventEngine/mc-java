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
import io.spine.protodata.renderer.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.mc.java.DirectMessageAction
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.OverrideAnnotation
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addLast
import org.intellij.lang.annotations.Language

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
public class AddCompareToMethod(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : DirectMessageAction<Empty>(type, file, Empty.getDefaultInstance(), context) {

    @Language("JAVA") @Suppress("EmptyClass")
    override fun doRender() {
        val clsName = cls.name!!
        val lowercasedClsName = clsName.replaceFirstChar { it.lowercaseChar() }
        val method = elementFactory.createMethodFromText(
            """
            public int compareTo($clsName $lowercasedClsName) {
                return comparator.compare(this, $lowercasedClsName);                          
            }            
            """.trimIndent(), cls
        )
        method.run {
            addFirst(GeneratedAnnotation.create())
            addFirst(OverrideAnnotation.create())
        }
        cls.addLast(method)
    }
}
