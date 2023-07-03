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
package io.spine.tools.mc.java.rejection.v2

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import io.spine.base.RejectionThrowable
import io.spine.logging.WithLogging
import io.spine.protodata.MessageType
import io.spine.tools.java.code.GeneratedBy
import io.spine.tools.java.code.field.FieldName
import io.spine.tools.java.javadoc.JavadocText
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC
import com.squareup.javapoet.ClassName as PoClassName

/**
 * A spec for a generated rejection type.
 *
 * The generated type extends [RejectionThrowable] and encloses an instance of the
 * corresponding [rejection message][io.spine.base.RejectionMessage].
 *
 * @param rejection
 *         a rejection declaration.
 */
internal class KRThrowableSpec(
    val packageName: String,
    val rejection: MessageType,
    typeSystem: TypeSystem
) : WithLogging {

    private val simpleClassName: String = rejection.name.simpleName
    private val messageClass: PoClassName
    private val builder: KRThrowableBuilderSpec

    init {
        val clsName = typeSystem.classNameFor(rejection.name).canonical
        messageClass = PoClassName.bestGuess(clsName)
        val throwableClass = PoClassName.get(packageName, simpleClassName)
        builder = KRThrowableBuilderSpec(
            rejection,
            messageClass,
            throwableClass,
            typeSystem
        )
    }

    fun toPoet(): TypeSpec {
        return TypeSpec.classBuilder(simpleClassName)
            .addJavadoc(classJavadoc())
            .addAnnotation(GeneratedBy.spineModelCompiler())
            .addModifiers(PUBLIC)
            .superclass(RejectionThrowable::class.java)
            .addField(serialVersionUID())
            .addMethod(constructor())
            .addMethod(messageThrown())
            .addMethod(builder.newBuilder())
            .addType(builder.toPoet())
            .build()
    }

    private fun constructor(): MethodSpec {
        logger.atDebug().log {
            "Creating the constructor for the type `${rejection}`."
        }
        val builderParameter = builder.asParameter()
        val buildRejectionMessage = builder.buildRejectionMessage()
        return MethodSpec.constructorBuilder()
            .addJavadoc(constructorJavadoc(builderParameter))
            .addModifiers(PRIVATE)
            .addParameter(builderParameter)
            .addStatement("super(\$L)", buildRejectionMessage.toString())
            .build()
    }

    private fun messageThrown(): MethodSpec {
        val methodSignature = messageThrown.signature()
        logger.atDebug().log {
            "Adding method `$methodSignature`."
        }
        val returnType = messageClass
        return MethodSpec.methodBuilder(messageThrown.name())
            .addAnnotation(Override::class.java)
            .addModifiers(PUBLIC)
            .returns(returnType)
            .addStatement("return (\$T) super.\$L", returnType, methodSignature)
            .build()
    }

    /**
     * A Javadoc content for the rejection.
     *
     * @return the class-level Javadoc content
     */
    private fun classJavadoc(): CodeBlock {
        val leadingComments = with(rejection.doc.leadingComment) {
            if (isEmpty()) {
                JavadocText.fromEscaped("")
            } else {
                JavadocText.fromUnescaped(this)
                    .inPreTags()
                    .withNewLine()
            }
        }
        val sourceProtoNote = CodeBlock.builder()
            .add("Rejection based on proto type ")
            .add("{@code \$L.\$L}", packageName, simpleClassName)
            .build()
        return CodeBlock.builder()
            .add(leadingComments.value())
            .add(JavadocText.fromEscaped(sourceProtoNote.toString()).withNewLine().value())
            .build()
    }
}

private val messageThrown = NoArgMethod("messageThrown")

/**
 * A Javadoc content for the rejection constructor.
 *
 * @param builderParameter
 *          the name of a rejection builder parameter
 * @return the constructor Javadoc content
 */
private fun constructorJavadoc(builderParameter: ParameterSpec): CodeBlock {
    val generalPart = JavadocText.fromUnescaped("Creates a new instance.")
        .withNewLine()
        .withNewLine()
    val paramsBlock =
        CodeBlock.of("@param \$N the builder for the rejection", builderParameter)
    val paramsPart = JavadocText.fromEscaped(paramsBlock.toString()).withNewLine()
    return CodeBlock.builder()
        .add(generalPart.value())
        .add(paramsPart.value())
        .build()
}

private fun serialVersionUID(): FieldSpec {
    return FieldSpec.builder(
        Long::class.javaPrimitiveType,
        FieldName.serialVersionUID().value(), PRIVATE, STATIC, FINAL)
        .initializer("0L")
        .build()
}

