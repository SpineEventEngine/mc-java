/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.protodata.codegen.java

import io.spine.protodata.Field
import io.spine.protodata.PrimitiveType
import io.spine.protodata.ProtoFileHeader
import io.spine.protodata.Type
import io.spine.protodata.TypeName
import io.spine.protodata.isMap
import io.spine.protodata.isMessage
import io.spine.protodata.isEnum
import io.spine.protodata.isPrimitive
import io.spine.protodata.isRepeated
import io.spine.protodata.type.TypeSystem
import io.spine.type.shortDebugString

//TODO:2024-02-10:alexander.yevsyukov: Move to `AstJavaExts.kt` in ProtoData.

/**
 * Obtains a fully qualified name of this type in the context of the given [TypeSystem].
 *
 * If this type [isPrimitive], its name does not depend on [TypeSystem] and
 * the result of [toPrimitiveName][io.spine.protodata.PrimitiveType.toPrimitiveName]
 * is returned.
 *
 * @param typeSystem
 *         the type system to use for resolving the Java type.
 * @throws IllegalStateException
 *         if the field type cannot be converted to a Java counterpart.
 */
@Suppress("ReturnCount") // Sooner exit is important!
public fun Type.javaType(typeSystem: TypeSystem): String {
    if (isPrimitive) {
        return primitiveClassName()
    }
    val declaredIn = typeSystem.findHeader(this)
    check(declaredIn != null) {
        "Unable to locate a header of the file declaring the type `${shortDebugString()}`."
    }
    return javaClassName(declaredIn)
}

private fun Type.primitiveClassName(): String {
    check(isPrimitive) {
        error("The type is not primitive: `${shortDebugString()}`.")
    }
    return primitive.primitiveClass().javaObjectType.simpleName
}

/**
 * Finds a header of the file which declares the given type.
 */
public fun TypeSystem.findHeader(type: Type): ProtoFileHeader? {
    require(type.isMessage || type.isEnum) {
        "The type must be either a message or an enum. Passed: `${type.shortDebugString()}`."
    }
    val typeName = type.typeName()
    val found = when {
        type.isMessage -> findMessage(typeName)
        type.isEnum -> findEnum(typeName)
        else -> null // Cannot happen.
    }
    return found?.second
}

private fun Type.typeName(): TypeName {
    val typeName = when {
        isMessage -> message
        isEnum -> enumeration
        else -> error("Unable to convert a primitive type `${primitive.name}` to `TypeName`.")
    }
    return typeName
}

/**
 * Obtains a name of a Java class which corresponds to values with this type.
 */
public fun Type.javaClassName(accordingTo: ProtoFileHeader): String = when {
    isPrimitive -> primitiveClassName()
    isMessage -> message.javaClassName(accordingTo).canonical
    isEnum -> enumeration.javaClassName(accordingTo).canonical
    else -> error("Unable to convert the type `$this` to Java counterpart.")
}

/**
 * Obtains the Java type of the field in the context of the given [TypeSystem].
 *
 * The returned type may have generic parameters, if the field is `repeated` or a `map`.
 *
 * @param typeSystem
 *         the type system to use for resolving the Java type.
 * @return the fully qualified reference to the Java type of the field.
 * @throws IllegalStateException
 *         if the field type cannot be converted to a Java counterpart.
 */
public fun Field.javaType(typeSystem: TypeSystem): String = when {
    isMap -> typeSystem.mapType(map.keyType, type)
    isRepeated -> typeSystem.repeatedType(type)
    else -> type.javaType(typeSystem)
}

private fun TypeSystem.mapType(key: PrimitiveType, value: Type): String {
    val keyType = key.primitiveClass()
    val valueType = value.javaType(this)
    return "${java.util.Map::class.java.canonicalName}<$keyType, $valueType>"
}

private fun TypeSystem.repeatedType(element: Type): String {
    val javaType = element.javaType(this)
    return "${java.util.List::class.java.canonicalName}<$javaType>"
}
