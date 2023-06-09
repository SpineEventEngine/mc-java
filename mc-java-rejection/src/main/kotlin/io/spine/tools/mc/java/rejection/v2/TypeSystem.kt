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

import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import io.spine.protodata.EnumType
import io.spine.protodata.File
import io.spine.protodata.MessageType
import io.spine.protodata.PrimitiveType
import io.spine.protodata.PrimitiveType.PT_UNKNOWN
import io.spine.protodata.PrimitiveType.TYPE_BOOL
import io.spine.protodata.PrimitiveType.TYPE_BYTES
import io.spine.protodata.PrimitiveType.TYPE_DOUBLE
import io.spine.protodata.PrimitiveType.TYPE_FIXED32
import io.spine.protodata.PrimitiveType.TYPE_FIXED64
import io.spine.protodata.PrimitiveType.TYPE_FLOAT
import io.spine.protodata.PrimitiveType.TYPE_INT32
import io.spine.protodata.PrimitiveType.TYPE_INT64
import io.spine.protodata.PrimitiveType.TYPE_SFIXED32
import io.spine.protodata.PrimitiveType.TYPE_SFIXED64
import io.spine.protodata.PrimitiveType.TYPE_SINT32
import io.spine.protodata.PrimitiveType.TYPE_SINT64
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.PrimitiveType.TYPE_UINT32
import io.spine.protodata.PrimitiveType.TYPE_UINT64
import io.spine.protodata.PrimitiveType.UNRECOGNIZED
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.Type
import io.spine.protodata.Type.KindCase.ENUMERATION
import io.spine.protodata.Type.KindCase.MESSAGE
import io.spine.protodata.Type.KindCase.PRIMITIVE
import io.spine.protodata.TypeName
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.javaClassName
import io.spine.protodata.name
import io.spine.type.KnownTypes
import kotlin.reflect.KClass

/**
 * A type system of an application.
 *
 * Includes all the types known to the app at runtime.
 */
public class TypeSystem
private constructor(
    private val knownTypes: Map<TypeName, ClassName>
) {

    public companion object {

        /**
         * Creates a new `TypeSystem` builder.
         */
        @JvmStatic
        public fun newBuilder(): Builder = Builder()
    }

    /**
     * Obtains the name of the Java class generated from a Protobuf type with the given name.
     */
    public fun javaTypeName(type: Type): String {
        return when {
            type.hasPrimitive() -> type.primitive.toPrimitiveName()
            type.hasMessage() -> classNameFor(type.message).canonical
            type.hasEnumeration() -> classNameFor(type.enumeration).canonical
            else -> unknownType(type)
        }
    }

    /**
     * Obtains the name of the class from a given name of a Protobuf type.
     */
    internal fun classNameFor(type: TypeName) =
        knownTypes[type] ?: unknownType(type)

    /**
     * The builder of a new `TypeSystem` of an application.
     */
    public class Builder internal constructor() {

        private val knownTypes = mutableMapOf<TypeName, ClassName>()

        init {
            KnownTypes.instance()
                .asTypeSet()
                .messagesAndEnums()
                .forEach {
                    knownTypes[it.typeName()] = ClassName(it.javaClass())
                }
        }

        private fun io.spine.type.Type<*, *>.typeName(): TypeName {
            return when (val descriptor = descriptor()) {
                is Descriptor -> descriptor.name()
                is EnumDescriptor -> descriptor.name()
                else -> error("Unexpected type: `$descriptor`.")
            }
        }

        @CanIgnoreReturnValue
        public fun put(file: File, messageType: MessageType): Builder {
            val javaClassName = messageType.javaClassName(declaredIn = file)
            knownTypes[messageType.name] = javaClassName
            return this
        }

        @CanIgnoreReturnValue
        public fun put(file: File, enumType: EnumType): Builder {
            val javaClassName = enumType.javaClassName(declaredIn = file)
            knownTypes[enumType.name] = javaClassName
            return this
        }

        /**
         * Adds all the definitions from the given `file` to the type system.
         */
        @CanIgnoreReturnValue
        public fun addFrom(file: ProtobufSourceFile): Builder {
            file.typeMap.values.forEach {
                put(file.file, it)
            }
            file.enumTypeMap.values.forEach {
                put(file.file, it)
            }
            return this
        }

        /**
         * Builds an instance of `TypeSystem`.
         */
        public fun build(): TypeSystem = TypeSystem(knownTypes)
    }
}

/**
 * Obtains the canonical name of the class representing the given [type] in Java.
 *
 * For Java primitive types, obtains wrapper classes.
 *
 * @throws IllegalStateException if the type is unknown
 */
private fun TypeSystem.toClass(type: Type): ClassName = when (type.kindCase) {
    PRIMITIVE -> type.primitive.toClass()
    MESSAGE, ENUMERATION -> classNameFor(type.message)
    else -> throw IllegalArgumentException("Type is empty.")
}

private fun unknownType(type: Type): Nothing =
    error("Unknown type: `${type}`.")

private fun unknownType(typeName: TypeName): Nothing =
    error("Unknown type: `${typeName.typeUrl}`.")


/**
 * Obtains a name of the class which corresponds to this primitive type.
 */
internal fun PrimitiveType.toClass(): ClassName {
    val klass = primitiveClass()
    return ClassName(klass.javaObjectType)
}

/**
 * Obtains a name of the class which corresponds to this primitive type.
 */
internal fun PrimitiveType.toPrimitiveName(): String {
    val klass = primitiveClass()
    val primitiveClass = klass.javaPrimitiveType ?: klass.java
    return primitiveClass.name
}

private fun PrimitiveType.primitiveClass(): KClass<*> =
    when (this) {
        TYPE_DOUBLE -> Double::class
        TYPE_FLOAT -> Float::class

        TYPE_INT64, TYPE_UINT64, TYPE_SINT64,
        TYPE_FIXED64, TYPE_SFIXED64 -> Long::class

        TYPE_INT32, TYPE_UINT32, TYPE_SINT32,
        TYPE_FIXED32, TYPE_SFIXED32 -> Int::class

        TYPE_BOOL -> Boolean::class
        TYPE_STRING -> String::class
        TYPE_BYTES -> ByteString::class
        UNRECOGNIZED, PT_UNKNOWN -> unknownType(this)
    }

private fun unknownType(type: PrimitiveType): Nothing {
    error("Unknown primitive type: `$type`.")
}
