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
@file:Suppress("TooManyFunctions")

package io.spine.tools.mc.java.rejection

import com.google.protobuf.ByteString
import com.squareup.javapoet.AnnotationSpec
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
import io.spine.protodata.java.MessageOrEnumConvention
import io.spine.protodata.java.RejectionThrowableConvention
import io.spine.protodata.isMap
import io.spine.protodata.isRepeated
import io.spine.protodata.type.TypeSystem
import io.spine.string.titleCase
import io.spine.tools.java.classSpec
import io.spine.tools.java.code.BuilderSpec
import io.spine.tools.java.code.BuilderSpec.RETURN_STATEMENT
import io.spine.tools.java.codeBlock
import io.spine.tools.java.constructorSpec
import io.spine.tools.java.javadoc.JavadocText
import io.spine.tools.java.methodSpec
import io.spine.tools.mc.java.field.RepeatedFieldType
import io.spine.tools.mc.java.field.SingularFieldType.constructTypeNameFor
import io.spine.tools.mc.java.rejection.Javadoc.forBuilderOf
import io.spine.tools.mc.java.rejection.Javadoc.ofBuildMethod
import io.spine.tools.mc.java.rejection.Javadoc.ofBuilderConstructor
import io.spine.tools.mc.java.rejection.Javadoc.ofNewBuilderMethod
import io.spine.tools.mc.java.rejection.Javadoc.ofRejectionMessageMethod
import io.spine.tools.mc.java.rejection.Method.BUILD
import io.spine.tools.mc.java.rejection.Method.NEW_BUILDER
import io.spine.tools.mc.java.rejection.Method.REJECTION_MESSAGE
import io.spine.validate.Validate
import io.spine.validate.Validated
import java.util.regex.Pattern
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC
import kotlin.reflect.KClass
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
    typeSystem: TypeSystem
) : BuilderSpec {

    private val messageOrEnumConvention = MessageOrEnumConvention(typeSystem)
    private val rejectionConvention = RejectionThrowableConvention(typeSystem)

    private val simpleClassName: String = SimpleClassName.ofBuilder().value

    override fun packageName(): PackageName = rejection.javaPackage()

    override fun toPoet(): TypeSpec = classSpec(simpleClassName) {
        addModifiers(PUBLIC, STATIC)
        addJavadoc(forBuilderOf(rejection))
        addField(messageClass.builderField())
        addMethod(constructor())
        addMethods(setters())
        addMethod(messageClass.rejectionMessageMethod())
        addMethod(throwableClass.buildMethod())
    }

    private fun MessageType.javaPackage(): PackageName {
        val binaryName = rejectionConvention.declarationFor(this@javaPackage.name)!!.name.binary
        return PackageName.of(binaryName.substringBeforeLast("."))
    }

    /**
     * Obtains the method to create the builder.
     */
    fun newBuilder(): MethodSpec = methodSpec(newBuilder.name()) {
        addModifiers(PUBLIC, STATIC)
        addJavadoc(ofNewBuilderMethod)
        returns(builderClass())
        addStatement("return new \$L()", simpleClassName)
    }

    /**
     * Obtains the builder class as a parameter.
     */
    fun asParameter(): ParameterSpec =
        ParameterSpec.builder(builderClass(), BUILDER_FIELD).build()

    /**
     * A code block, which builds and validates the rejection message.
     *
     * The code block is not a statement (there is no semicolon) since
     * it is intended to be passes to a constructor.
     *
     * @return the code block to obtain a rejection message
     */
    fun buildRejectionMessage(): CodeBlock = codeBlock {
        add("\$N.\$N()", asParameter(), messageClass.rejectionMessageMethod())
    }

    private fun setters(): List<MethodSpec> = buildList {
        for (field in rejection.fieldList) {
            add(field.setterMethod())
        }
    }

    private fun Field.setterMethod(): MethodSpec = methodSpec(primarySetterName()) {
        val parameterName = name.javaCase()
        addModifiers(PUBLIC)
        returns(builderClass())
        addParameter(poetTypeName(), parameterName)
        addStatement("\$L.\$L(\$L)", BUILDER_FIELD, primarySetterName(), parameterName)
        addStatement(RETURN_STATEMENT)

        // Add line separator to simulate behavior of native Protobuf API.
        val leadingComment = doc.leadingComment + System.lineSeparator()

        if (leadingComment.isNotEmpty()) {
            addJavadoc(wrapInPre(leadingComment))
        }
    }

    private fun Field.poetTypeName(): PoTypeName {
        return when {
            isMap -> mapType(map.keyType, type)
            isRepeated -> repeatedNameOf(type)
            else -> singularNameOf(type)
        }
    }

    private fun singularNameOf(type: Type): PoTypeName {
        val javaType = when {
            type.hasMessage() ->
                messageOrEnumConvention.declarationFor(type.message).name.canonical
            type.hasEnumeration() ->
                messageOrEnumConvention.declarationFor(type.enumeration).name.canonical
            type.hasPrimitive() ->
                type.primitive.toPrimitiveName()
            else -> {
                unknownType(type)
            }
        }
        return typeNameOf(javaType)
    }

    private fun repeatedNameOf(type: Type): PoTypeName {
        val elementType = javaTypeName(type)
        return RepeatedFieldType.typeNameFor(elementType)
    }

    private fun mapType(keyType: PrimitiveType, valueType: Type): PoTypeName {
        val keyTypeName = singularNameOf(keyType)
        val valueTypeName = singularNameOf(valueType)
        val result = ParameterizedTypeName.get(
            ClassName.get(MutableMap::class.java), keyTypeName, valueTypeName
        )
        return result
    }

    /**
     * Obtains the class name of the builder to generate.
     */
    private fun builderClass(): ClassName =
        throwableClass.nestedClass(simpleClassName)

    /**
     * Obtains the name of the Java class generated from a Protobuf type with the given name.
     */
    private fun javaTypeName(type: Type): String {
        return when {
            type.hasPrimitive() ->
                type.primitive.toPrimitiveName()
            type.hasMessage() ->
                messageOrEnumConvention.declarationFor(type.message).name.canonical
            type.hasEnumeration() ->
                messageOrEnumConvention.declarationFor(type.enumeration).name.canonical
            else -> unknownType(type)
        }
    }
}

private val newBuilder = NoArgMethod(NEW_BUILDER)
private const val BUILDER_FIELD = "builder"

private fun constructor(): MethodSpec = constructorSpec {
    addJavadoc(ofBuilderConstructor)
    addModifiers(PRIVATE)
}

private fun wrapInPre(text: String): String =
    JavadocText.fromUnescaped(text)
        .inPreTags()
        .value()

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

private fun Field.primarySetterName(): String {
    val capName = name.javaCase().titleCase()
    return when {
        isMap -> "putAll$capName"
        isRepeated -> "addAll$capName"
        else -> "set$capName"
    }
}

private fun singularNameOf(primitiveType: PrimitiveType): PoTypeName =
    typeNameOf(primitiveType.toPrimitiveName())

private fun typeNameOf(javaType: String): PoTypeName =
    constructTypeNameFor(javaType).boxIfPrimitive()

private fun PoTypeName.boxIfPrimitive(): PoTypeName = if (isPrimitive) box() else this

private fun PoClassName.builderField(): FieldSpec {
    val builder = SimpleClassName.ofBuilder().value()
    val builderClass = nestedClass(builder)
    return FieldSpec
        .builder(builderClass, BUILDER_FIELD, PRIVATE, FINAL)
        .initializer("\$T.newBuilder()", this)
        .build()
}

private fun PoClassName.buildMethod(): MethodSpec = methodSpec(BUILD) {
    val messageClass = this@buildMethod
    addModifiers(PUBLIC)
    addJavadoc(ofBuildMethod)
    returns(messageClass)
    addStatement("return new \$T(this)", messageClass)
}

/**
 * Generates the code for the method named as defined by the [REJECTION_MESSAGE] constant.
 *
 * The method creates a new instance by calling the builder's `build()` method.
 * Then it validates the new instance via [Validate.check] and returns it.
 */
private fun PoClassName.rejectionMessageMethod(): MethodSpec = methodSpec(REJECTION_MESSAGE) {
    val messageType = this@rejectionMessageMethod
    val annotatedType = messageType.annotated(
        AnnotationSpec.builder(Validated::class.java).build()
    )
    addModifiers(PRIVATE)
    addJavadoc(ofRejectionMessageMethod)
    returns(annotatedType)
    addStatement("return \$L.build()", BUILDER_FIELD)
}

/**
 * Obtains a name of the class which corresponds to this primitive type.
 */
private fun PrimitiveType.toPrimitiveName(): String {
    val klass = primitiveClass()
    val primitiveClass = klass.javaPrimitiveType ?: klass.java
    return primitiveClass.name
}

private fun PrimitiveType.primitiveClass(): KClass<*> =
    when (this) {
        PrimitiveType.TYPE_DOUBLE -> Double::class
        PrimitiveType.TYPE_FLOAT -> Float::class

        PrimitiveType.TYPE_INT64, PrimitiveType.TYPE_UINT64, PrimitiveType.TYPE_SINT64,
        PrimitiveType.TYPE_FIXED64, PrimitiveType.TYPE_SFIXED64 -> Long::class

        PrimitiveType.TYPE_INT32, PrimitiveType.TYPE_UINT32, PrimitiveType.TYPE_SINT32,
        PrimitiveType.TYPE_FIXED32, PrimitiveType.TYPE_SFIXED32 -> Int::class

        PrimitiveType.TYPE_BOOL -> Boolean::class
        PrimitiveType.TYPE_STRING -> String::class
        PrimitiveType.TYPE_BYTES -> ByteString::class
        PrimitiveType.UNRECOGNIZED, PrimitiveType.PT_UNKNOWN -> unknownType(this)
    }

private fun unknownType(type: PrimitiveType): Nothing {
    error("Unknown primitive type: `$type`.")
}

private fun unknownType(type: Type): Nothing =
    error("Unknown type: `${type}`.")
