/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.routing

import com.google.devtools.ksp.symbol.ClassKind.CLASS
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

internal class EntityClass(
    private val decl: KSClassDeclaration,
    entityInterface: KSType
) {
    fun accept(visitor: RouteVisitor<*>, data: Unit) {
        decl.accept(visitor, data)
    }

    val type: KSType by lazy { decl.asStarProjectedType() }

    val idClass: KSType by lazy {
        val asEntity = decl.superTypes.find {
            entityInterface.isAssignableFrom(it.resolve())
        }
        check(asEntity != null) {
            "The class `${decl.qualifiedName!!.asString()}`" +
                    " must implement ${entityInterface.declaration.qualifiedName!!.asString()}`."
        }
        val firstTypeArgument = asEntity.element!!.typeArguments.first()
        firstTypeArgument.type!!.resolve()
    }

    fun superClass(): KSType {
        val found = decl.superTypes.find {
            val superType = it.resolve().declaration
            (superType is KSClassDeclaration) && (superType.classKind == CLASS)
        }
        return found!!.resolve()
    }
}
