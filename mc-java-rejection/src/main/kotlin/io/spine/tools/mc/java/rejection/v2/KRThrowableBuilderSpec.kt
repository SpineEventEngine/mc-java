/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import io.spine.base.RejectionType
import io.spine.code.java.PackageName
import io.spine.code.java.SimpleClassName
import io.spine.code.proto.FieldDeclaration
import io.spine.tools.java.code.BuilderSpec
import io.spine.tools.java.code.JavaPoetName
import io.spine.tools.java.code.field.FieldName
import io.spine.tools.java.javadoc.JavadocText
import io.spine.tools.mc.java.field.FieldType
import io.spine.validate.Validate
import javax.lang.model.element.Modifier

/**
 * Generates code for a rejection builder.
 *
 *
 * A generated builder validates rejection messages using
 * [Validate.check].
 */
internal class KRThrowableBuilderSpec internal constructor(
    private val rejection: RejectionType,
    private val messageClass: JavaPoetName,
    private val throwableClass: JavaPoetName
) : BuilderSpec {
    private val name: SimpleClassName

    init {
        name = SimpleClassName.ofBuilder()
    }

    override fun packageName(): PackageName {
        return rejection.javaPackage()
    }

    override fun toPoet(): TypeSpec {
        return TypeSpec.classBuilder(name.value())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc(classJavadoc().value())
            .addField(initializedProtoBuilder())
            .addMethod(constructor())
            .addMethods(setters())
            .addMethod(rejectionMessage())
            .addMethod(build())
            .build()
    }

    /**
     * Obtains the method to create the builder.
     *
     * @return the `newInstance` specification
     */
    fun newBuilder(): MethodSpec {
        val javadoc =
            JavadocText.fromEscaped("@return a new builder for the rejection").withNewLine()
        return MethodSpec.methodBuilder(newBuilder.name())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc(javadoc.value())
            .returns(thisType())
            .addStatement("return new \$L()", name.value())
            .build()
    }

    /**
     * Obtains the builder as a parameter.
     *
     * @return the parameter specification for this builder
     */
    fun asParameter(): ParameterSpec {
        return ParameterSpec
            .builder(thisType(), BUILDER_FIELD)
            .build()
    }

    /**
     * A code block, which builds and validates the rejection message.
     *
     *
     * The code block is not a statement (there is no semicolon) since
     * it is intended to be passes to a constructor.
     *
     * @return the code block to obtain a rejection message
     */
    fun buildRejectionMessage(): CodeBlock {
        return CodeBlock
            .builder()
            .add("\$N.\$N()", asParameter(), rejectionMessage())
            .build()
    }

    private fun rejectionMessage(): MethodSpec {
        val javadoc =
            JavadocText.fromEscaped("Obtains the rejection and validates it.").withNewLine()
        return MethodSpec.methodBuilder("rejectionMessage")
            .addModifiers(Modifier.PRIVATE)
            .addJavadoc(javadoc.value())
            .returns(messageClass.value())
            .addStatement("\$T message = \$L.build()", messageClass.value(), BUILDER_FIELD)
            .addStatement("\$T.check(message)", Validate::class.java)
            .addStatement("return message")
            .build()
    }

    private fun build(): MethodSpec {
        val rawJavadoc = "Creates the rejection from the builder and validates it."
        val javadoc = JavadocText.fromEscaped(rawJavadoc).withNewLine()
        return MethodSpec.methodBuilder(BuilderSpec.BUILD_METHOD_NAME)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc(javadoc.value())
            .returns(throwableClass.value())
            .addStatement("return new \$T(this)", throwableClass.value())
            .build()
    }

    private fun classJavadoc(): JavadocText {
        val rejectionName = rejection.simpleJavaClassName().value()
        val javadocText = CodeBlock.builder()
            .add("The builder for the {@code \$L} rejection.", rejectionName)
            .build()
            .toString()
        return JavadocText.fromEscaped(javadocText)
            .withNewLine()
    }

    // Random generated code duplication.
    private fun initializedProtoBuilder(): FieldSpec {
        val builder = SimpleClassName.ofBuilder().value()
        val builderClass = messageClass.className().nestedClass(builder)
        return FieldSpec
            .builder(builderClass, BUILDER_FIELD, Modifier.PRIVATE, Modifier.FINAL)
            .initializer("\$T.newBuilder()", messageClass.value())
            .build()
    }

    private fun setters(): List<MethodSpec> {
        val methods: MutableList<MethodSpec> = ArrayList()
        val fields = rejection.fields()
        for (field in fields) {
            val fieldType = FieldType.of(field)
            val setter = fieldSetter(field, fieldType)
            methods.add(setter)
        }
        return methods
    }

    private fun fieldSetter(field: FieldDeclaration, fieldType: FieldType): MethodSpec {
        val fieldName = field.name()
        val parameterName = fieldName.javaCase()
        val methodName = fieldType.primarySetter()
            .format(FieldName.from(fieldName))
        val methodBuilder = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC)
            .returns(thisType())
            .addParameter(fieldType.name(), parameterName)
            .addStatement("\$L.\$L(\$L)", BUILDER_FIELD, methodName, parameterName)
            .addStatement(BuilderSpec.RETURN_STATEMENT)
        field.leadingComments()
            .map { text: String -> wrapInPre(text) }
            .ifPresent { format: String? -> methodBuilder.addJavadoc(format) }
        return methodBuilder.build()
    }

    /**
     * Obtains the class name of this builder.
     *
     * @return class name for the builder
     */
    private fun thisType(): ClassName {
        return throwableClass.className()
            .nestedClass(name.value())
    }

    companion object {
        @VisibleForTesting
        val NEW_BUILDER_METHOD = "newBuilder"
        private val newBuilder = NoArgMethod(NEW_BUILDER_METHOD)
        private const val BUILDER_FIELD = "builder"
        private fun constructor(): MethodSpec {
            val rawJavadoc = "Prevent direct instantiation of the builder."
            val javadoc = JavadocText.fromEscaped(rawJavadoc).withNewLine()
            return MethodSpec.constructorBuilder()
                .addJavadoc(javadoc.value())
                .addModifiers(Modifier.PRIVATE)
                .build()
        }

        private fun wrapInPre(text: String): String {
            val javaDoc = JavadocText.fromUnescaped(text).inPreTags()
            return javaDoc.value()
        }
    }
}
