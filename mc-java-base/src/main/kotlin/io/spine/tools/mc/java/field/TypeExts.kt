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

package io.spine.tools.mc.java.field

import com.google.common.base.Converter
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import io.spine.code.proto.FieldDeclaration
import io.spine.protodata.Doc
import io.spine.protodata.Field
import io.spine.protodata.FieldName
import io.spine.protodata.PrimitiveType
import io.spine.protodata.TypeName
import io.spine.type.EnumType
import io.spine.protodata.PrimitiveType as PPrimitiveType

/**
 * A converter between Protobuf primitive field type and its counterpart in ProtoData API.
 */
public object TypeConverter : Converter<Type, PPrimitiveType>() {

    override fun doForward(a: Type): PPrimitiveType {
        return when (a) {
            Type.BOOL -> PPrimitiveType.TYPE_BOOL
            Type.BYTES -> PPrimitiveType.TYPE_BYTES
            Type.DOUBLE -> PPrimitiveType.TYPE_DOUBLE
            Type.FIXED32 -> PPrimitiveType.TYPE_FIXED32
            Type.FIXED64 -> PPrimitiveType.TYPE_FIXED64
            Type.FLOAT -> PPrimitiveType.TYPE_FLOAT
            Type.INT32 -> PPrimitiveType.TYPE_INT32
            Type.INT64 -> PPrimitiveType.TYPE_INT64
            Type.SFIXED32 -> PPrimitiveType.TYPE_SFIXED32
            Type.SFIXED64 -> PPrimitiveType.TYPE_SFIXED64
            Type.SINT32 -> PPrimitiveType.TYPE_SINT32
            Type.SINT64 -> PPrimitiveType.TYPE_SINT64
            Type.STRING -> PPrimitiveType.TYPE_STRING
            Type.UINT32 -> PPrimitiveType.TYPE_UINT32
            Type.UINT64 -> PPrimitiveType.TYPE_UINT64
            else -> throw IllegalArgumentException(
                "Unable to convert the type `$a` as primitive one."
            )
        }
    }

    override fun doBackward(b: PPrimitiveType): Type {
        return when(b) {
            PPrimitiveType.TYPE_BOOL -> Type.BOOL
            PPrimitiveType.TYPE_BYTES -> Type.BYTES
            PPrimitiveType.TYPE_DOUBLE -> Type.DOUBLE
            PPrimitiveType.TYPE_FIXED32 -> Type.FIXED32
            PPrimitiveType.TYPE_FIXED64 -> Type.FIXED64
            PPrimitiveType.TYPE_FLOAT -> Type.FLOAT
            PPrimitiveType.TYPE_INT32 -> Type.INT32
            PPrimitiveType.TYPE_INT64 -> Type.INT64
            PPrimitiveType.TYPE_SFIXED32 -> Type.SFIXED32
            PPrimitiveType.TYPE_SFIXED64 -> Type.SFIXED64
            PPrimitiveType.TYPE_SINT32 -> Type.SINT32
            PPrimitiveType.TYPE_SINT64 -> Type.SINT64
            PPrimitiveType.TYPE_STRING -> Type.STRING
            PPrimitiveType.TYPE_UINT32 -> Type.UINT32
            PPrimitiveType.TYPE_UINT64 -> Type.UINT64
            else -> throw IllegalArgumentException(
                "Unable to convert the type `$b` to a Protobuf field type."
            )
        }
    }
}

public fun FieldDeclaration.toField(): Field {
    return Field.newBuilder()
        .setName(fieldName())
        .setType(type())
        .setNumber(number())
        .setDoc(doc())
        //TODO: Add other fields?
        .build()
}

private fun FieldDeclaration.doc(): Doc {
    val leadingComment = leadingComments().orElse("")
    val result = Doc.newBuilder().setLeadingComment(leadingComment)
    return result.build()
}

private fun FieldDeclaration.fieldName(): FieldName {
    return FieldName.newBuilder()
        .setValue(name().value())
        .build()
}

private fun FieldDeclaration.type(): io.spine.protodata.Type {
    if (isMessage) {
        return messageType().toType()
    }
    val typeBuilder = io.spine.protodata.Type.newBuilder()
    val desc = descriptor()
    if (isEnum) {
        val enumType = EnumType.create(desc.enumType)
        val typeName = TypeName.newBuilder()
            .setTypeUrlPrefix(enumType.url().prefix())
            .setPackageName(enumType.descriptor().file.getPackage())
            .setSimpleName(enumType.simpleJavaClassName().value())
            .build()
        typeBuilder.setEnumeration(typeName)
    } else {
        val primitiveType = desc.type.toType()
        typeBuilder.setPrimitive(primitiveType)
    }
    return typeBuilder.build()
}

private fun io.spine.type.MessageType.toType(): io.spine.protodata.Type {
    val descr = descriptor()
    val typeName = TypeName.newBuilder()
        .setTypeUrlPrefix(url().prefix())
        .setPackageName(descr.file.getPackage())
        .setSimpleName(simpleJavaClassName().value())
    var containingType = descr.containingType
    while (containingType != null) {
        typeName.addNestingTypeName(containingType.name)
        containingType = containingType.containingType
    }
    return io.spine.protodata.Type.newBuilder().setMessage(typeName).build()
}

private fun Descriptors.FieldDescriptor.Type.toType(): PrimitiveType {
    val result = TypeConverter.convert(this)
    return result!!
}
