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

import io.spine.protodata.Field
import io.spine.protodata.FieldName
import io.spine.protodata.TypeName
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.ClassOrEnumName
import io.spine.protodata.codegen.java.FieldConventions
import io.spine.protodata.codegen.java.MessageOrBuilderConvention
import io.spine.protodata.codegen.java.MessageOrEnumConvention
import io.spine.protodata.renderer.NonRepeatingInsertionPoint
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.type.Declaration
import io.spine.text.Text
import io.spine.text.TextCoordinates
import io.spine.tools.code.Java
import io.spine.tools.mc.annotation.ApiOption.Companion.findMatching
import io.spine.tools.mc.annotation.MessageAnnotations

internal class MessageAnnotationRenderer :
    AnnotationRenderer<MessageAnnotations>(MessageAnnotations::class.java) {

    private val convention by lazy {
        MessageOrEnumConvention(typeSystem!!)
    }

    private val messageOrBuilderConvention by lazy {
        MessageOrBuilderConvention(typeSystem!!)
    }

    override fun annotateType(state: MessageAnnotations, annotationClass: Class<out Annotation>) {
        val typeName = state.type

        val messageClass = convention.declarationFor(typeName)
        ApiTypeAnnotation(messageClass.name, annotationClass).let {
            it.registerWith(context!!)
            it.renderSources(sources)
        }

        val messageOrBuilderClass = messageOrBuilderConvention.declarationFor(typeName)
        ApiTypeAnnotation(messageOrBuilderClass.name, annotationClass).let {
            it.registerWith(context!!)
            it.renderSources(sources)
        }
    }

    override fun annotate(state: MessageAnnotations) {
        super.annotate(state)
        annotateFields(state)
    }

    private fun annotateFields(state: MessageAnnotations) {
        val typeName = state.type
        val messageDeclaration = MessageOrEnumConvention(typeSystem!!)
            .declarationFor(typeName)
        val messageOrBuilderDeclaration = MessageOrBuilderConvention(typeSystem!!)
            .declarationFor(typeName)

        state.fieldOptionsList.forEach { fieldOption ->
            fieldOption.optionList.forEach { option ->
                val annotationClass = findMatching(option)!!.annotationClass
                annotateFieldMethods(
                    typeName,
                    messageDeclaration,
                    messageOrBuilderDeclaration,
                    fieldOption.field,
                    annotationClass
                )
            }
        }
    }

    private fun annotateFieldMethods(
        typeName: TypeName,
        messageDeclaration: Declaration<Java, ClassOrEnumName>,
        messageOrBuilderDeclaration: Declaration<Java, ClassName>,
        fieldName: FieldName,
        annotationClass: Class<out Annotation>
    ) {
        val messageFile = sources.file(messageDeclaration.path)
        val messageType = typeSystem?.findMessage(typeName)!!.first
        val field = messageType.fieldList.find { it.name == fieldName }!!

        val annotationLine = "@${annotationClass.canonicalName}"
        val fieldGetter = FieldGetter(field)
        messageFile.at(fieldGetter).add(annotationLine)

        val messageOrBuilderFile = sources.file(messageOrBuilderDeclaration.path)
        messageOrBuilderFile.at(fieldGetter).add(annotationLine)
        // TODO: Annotate builder methods for setters too.
    }

    override fun suitableFor(sources: SourceFileSet): Boolean =
        sources.outputRoot.endsWith("java")
}

private class FieldGetter(private val field: Field) :
    NonRepeatingInsertionPoint,
    FieldConventions(field.name, field.cardinalityCase) {

    override val label: String
        get() = ""

    override fun locateOccurrence(text: Text): TextCoordinates {
        val regex = Regex("public .+ $getterName")
        text.lines().mapIndexed { index, line ->
            if (regex.containsMatchIn(line)) {
                return atLine(index)
            }
        }
        error("No getter found for field `${field.name.value}`.")
    }
}
