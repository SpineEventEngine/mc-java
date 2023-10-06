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

package io.spine.tools.mc.java.annotation

import io.spine.protodata.FieldName
import io.spine.protodata.Option
import io.spine.protodata.TypeName
import io.spine.protodata.codegen.java.MessageOrEnumConvention
import io.spine.protodata.renderer.SourceFile
import io.spine.tools.mc.annotation.MessageAnnotations

internal class MessageAnnotationRenderer :
    AnnotationRenderer<MessageAnnotations>(MessageAnnotations::class.java) {

    override fun annotateType(state: MessageAnnotations, annotationClass: Class<out Annotation>) {
        val annotation = MessageApiAnnotation(state.type, annotationClass)
        annotation.renderSources(sources)
    }

    override fun annotate(state: MessageAnnotations) {
        super.annotate(state)
        state.fieldOptionList.forEach { fieldOption ->
            fieldOption.optionList.forEach { option ->
                annotateFieldMethods(fieldOption.field, option)
            }
        }
    }

    private fun annotateFieldMethods(field: FieldName, option: Option) {
        println("Annotating field methods `$field` with option `$option`.")
    }
}

private class MessageApiAnnotation<T : Annotation>(
    private val typeName: TypeName,
    annotationClass: Class<T>
) : ApiTypeAnnotation<T>(annotationClass) {

    private val convention = MessageOrEnumConvention(typeSystem)

    override fun shouldAnnotate(file: SourceFile): Boolean {
        val declaration = convention.declarationFor(typeName)
        return declaration.path.endsWith(file.relativePath)
    }
}
