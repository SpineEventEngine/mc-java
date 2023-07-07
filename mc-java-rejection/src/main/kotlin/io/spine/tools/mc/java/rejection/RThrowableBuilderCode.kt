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
package io.spine.tools.mc.java.rejection

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import io.spine.code.java.PackageName
import io.spine.code.java.SimpleClassName
import io.spine.code.proto.UnderscoredName
import io.spine.protodata.Field
import io.spine.protodata.FieldName
import io.spine.protodata.MessageType
import io.spine.protodata.PrimitiveType
import io.spine.protodata.Type
import io.spine.protodata.isMap
import io.spine.protodata.isRepeated
import io.spine.string.titleCase
import io.spine.tools.java.code.BuilderSpec
import io.spine.tools.java.code.BuilderSpec.BUILD_METHOD_NAME
import io.spine.tools.java.code.BuilderSpec.RETURN_STATEMENT
import io.spine.tools.java.javadoc.JavadocText
import io.spine.tools.mc.java.field.RepeatedFieldType
import io.spine.tools.mc.java.field.SingularFieldType.constructTypeNameFor
import io.spine.tools.mc.java.rejection.RThrowableBuilderCode.Companion.NEW_BUILDER_METHOD_NAME
import io.spine.validate.Validate
import java.util.regex.Pattern
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC
import com.squareup.javapoet.ClassName as PoClassName
import com.squareup.javapoet.TypeName as PoTypeName

/**
 * Generates code for a rejection builder.
 *
 * A generated builder validates rejection messages using [Validate.check].
 */
internal class RThrowableBuilderCode internal constructor(
    private val rejection: MessageType,
    private val messageClass: PoClassName,
    private val throwableClass: PoClassName,
    private val typeSystem: TypeSystem
) : BuilderSpec {

    private val simpleClassName: SimpleClassName = SimpleClassName.ofBuilder()

    override fun packageName(): PackageName {
        return rejection.javaPackage()
    }

    override fun toPoet(): TypeSpec {
        return TypeSpec.classBuilder(simpleClassName.value())
            .addModifiers(PUBLIC, STATIC)
            .addJavadoc(classJavadoc().value())
            .addField(initializedProtoBuilder())
            .addMethod(constructor())
            .addMethods(setters())
            .addMethod(rejectionMessage())
            .addMethod(build())
            .build()
    }

    private fun MessageType.javaPackage(): PackageName {
        val binaryName = typeSystem.classNameFor(this@javaPackage.name).binary
        return PackageName.of(binaryName.substringBeforeLast("."))
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
            .addModifiers(PUBLIC, STATIC)
            .addJavadoc(javadoc.value())
            .returns(thisType())
            .addStatement("return new \$L()", simpleClassName.value())
            .build()
    }

    /**
     * Obtains the builder as a parameter.
     *
     * @return the parameter specification for this builder
     */
    fun asParameter(): ParameterSpec =
        ParameterSpec.builder(thisType(), BUILDER_FIELD).build()

    /**
     * A code block, which builds and validates the rejection message.
     *
     * The code block is not a statement (there is no semicolon) since
     * it is intended to be passes to a constructor.
     *
     * @return the code block to obtain a rejection message
     */
    fun buildRejectionMessage(): CodeBlock = CodeBlock.builder()
        .add("\$N.\$N()", asParameter(), rejectionMessage())
        .build()

    private fun rejectionMessage(): MethodSpec {
        val javadoc = JavadocText.fromEscaped(
            "Obtains the rejection and validates it."
        ).withNewLine()
        return MethodSpec.methodBuilder("rejectionMessage")
            .addModifiers(PRIVATE)
            .addJavadoc(javadoc.value())
            .returns(messageClass)
            .addStatement("\$T message = \$L.build()", messageClass, BUILDER_FIELD)
            .addStatement("\$T.check(message)", Validate::class.java)
            .addStatement("return message")
            .build()
    }

    private fun build(): MethodSpec {
        val javadoc = JavadocText.fromEscaped(
            "Creates the rejection from the builder and validates it."
        ).withNewLine()
        return MethodSpec.methodBuilder(BUILD_METHOD_NAME)
            .addModifiers(PUBLIC)
            .addJavadoc(javadoc.value())
            .returns(throwableClass)
            .addStatement("return new \$T(this)", throwableClass)
            .build()
    }

    private fun classJavadoc(): JavadocText {
        val rejectionName = rejection.name.simpleName
        val javadocText = CodeBlock.builder()
            .add("The builder for the {@code \$L} rejection.", rejectionName)
            .build()
            .toString()
        return JavadocText.fromEscaped(javadocText)
            .withNewLine()
    }

    private fun initializedProtoBuilder(): FieldSpec {
        val builder = SimpleClassName.ofBuilder().value()
        val builderClass = messageClass.nestedClass(builder)
        return FieldSpec
            .builder(builderClass, BUILDER_FIELD, PRIVATE, FINAL)
            .initializer("\$T.newBuilder()", messageClass)
            .build()
    }

    private fun setters(): List<MethodSpec> {
        val methods: MutableList<MethodSpec> = ArrayList()
        val fields = rejection.fieldList
        for (field in fields) {
            val setter = fieldSetter(field)
            methods.add(setter)
        }
        return methods
    }

    private fun fieldSetter(field: Field): MethodSpec {
        val parameterName = field.name.javaCase()
        val methodName = field.primarySetter()
        val methodBuilder = MethodSpec.methodBuilder(methodName)
            .addModifiers(PUBLIC)
            .returns(thisType())
            .addParameter(field.poetTypeName(), parameterName)
            .addStatement("\$L.\$L(\$L)", BUILDER_FIELD, methodName, parameterName)
            .addStatement(RETURN_STATEMENT)

        // Add line separator to simulate behavior of native Protobuf API.
        val leadingComment = field.doc.leadingComment + System.lineSeparator()

        if (leadingComment.isNotEmpty()) {
            methodBuilder.addJavadoc(wrapInPre(leadingComment))
        }
        return methodBuilder.build()
    }

    private fun Field.poetTypeName(): PoTypeName {
        return when {
            isMap() -> mapType(map.keyType, type)
            isRepeated() -> repeatedNameOf(type)
            else -> singularNameOf(type)
        }
    }

    private fun singularNameOf(type: Type): PoTypeName {
        val javaType = typeSystem.javaTypeName(type)
        return typeNameOf(javaType)
    }

    private fun repeatedNameOf(type: Type): PoTypeName {
        val elementType = typeSystem.javaTypeName(type)
        val result = RepeatedFieldType.typeNameFor(elementType)
        return result
    }

    private fun mapType(keyType: PrimitiveType, valueType: Type): PoTypeName {
        val keyTypeName = singularNameOf(keyType)
        val valueTypeName = singularNameOf(valueType)
        val result = ParameterizedTypeName.get(
            ClassName.get(MutableMap::class.java), keyTypeName, valueTypeName
        );
        return result
    }

    /**
     * Obtains the class name of this builder.
     *
     * @return class name for the builder
     */
    private fun thisType(): ClassName =
        throwableClass.nestedClass(simpleClassName.value())


    internal companion object {

        /**
         * The name of the `newBuilder()` method.
         */
        internal const val NEW_BUILDER_METHOD_NAME = "newBuilder"
    }
}

private val newBuilder = NoArgMethod(NEW_BUILDER_METHOD_NAME)
private const val BUILDER_FIELD = "builder"

private fun constructor(): MethodSpec {
    val javadoc = JavadocText.fromEscaped(
        "Prevent direct instantiation of the builder."
    ).withNewLine()
    return MethodSpec.constructorBuilder()
        .addJavadoc(javadoc.value())
        .addModifiers(PRIVATE)
        .build()
}

private fun wrapInPre(text: String): String {
    val javaDoc = JavadocText.fromUnescaped(text).inPreTags()
    return javaDoc.value()
}

/**
 * The separator is an underscore or a digit.
 *
 * A digit instead of an underscore should be kept in a word.
 * So, the second group is not just `(\\d)`.
 */
private const val WORD_SEPARATOR = "(_)|((?<=\\d)|(?=\\d))"
private val WORD_SEPARATOR_PATTERN = Pattern.compile(WORD_SEPARATOR)

private fun FieldName.asUnderscored() : UnderscoredName {
    return object : UnderscoredName {
        override fun words(): List<String> = WORD_SEPARATOR_PATTERN.split(value).toList()
        override fun value(): String = value
    }
}

private fun FieldName.javaCase(): String {
    val camelCase = asUnderscored().toCamelCase()
    return camelCase.replaceFirstChar { it.lowercaseChar() }
}

private fun Field.primarySetter(): String {
    val capName = name.javaCase().titleCase()
    return when {
        isMap() -> "putAll$capName"
        isRepeated() -> "addAll$capName"
        else -> "set$capName"
    }
}

private fun singularNameOf(primitiveType: PrimitiveType): PoTypeName {
    val javaType = primitiveType.toPrimitiveName()
    return typeNameOf(javaType)
}

private fun typeNameOf(javaType: String): PoTypeName {
    val po = constructTypeNameFor(javaType)
    return po.boxIfPrimitive()
}

private fun PoTypeName.boxIfPrimitive(): PoTypeName {
    return if (isPrimitive) box() else this
}
