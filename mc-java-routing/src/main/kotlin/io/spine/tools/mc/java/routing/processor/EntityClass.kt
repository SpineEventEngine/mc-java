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

package io.spine.tools.mc.java.routing.processor

import com.google.devtools.ksp.symbol.ClassKind.CLASS
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference

/**
 * Provides information about an entity class.
 *
 * @property decl The declaration of the class.
 * @param entityInterface The type of the [io.spine.server.entity.Entity]
 *  interface for resolving generic parameters.
 *  This is a supportive parameter that we pass instead of [Environment] instance
 *  to narrow down the dependencies of this class.
 */
internal class EntityClass(
    val decl: KSClassDeclaration,
    entityInterface: KSType
) {
    /**
     * Makes the given visitor visit the class declaration.
     */
    fun accept(visitor: RouteVisitor<*>, data: Unit) {
        decl.accept(visitor, data)
    }

    /**
     * The type of the entity class resolved without generic parameters.
     */
    val type: KSType by lazy { decl.asStarProjectedType() }

    /**
     * The type of the entity identifiers as [KSTypeArgument].
     */
    val idClassTypeArgument: KSTypeArgument by lazy {
        val asEntity = decl.superTypes.find {
            entityInterface.isAssignableFrom(it.resolve())
        }
        check(asEntity != null) {
            "The class `${decl.qualifiedName!!.asString()}`" +
                    " must implement ${entityInterface.declaration.qualified()}`."
        }
        asEntity.element!!.typeArguments.first()
    }

    /**
     * The reference to the ID class.
     */
    private val idClassReference: KSTypeReference by lazy {
        idClassTypeArgument.type!!
    }

    /**
     * The type of the entity identifiers.
     */
    val idClass: KSType by lazy {
        idClassReference.resolve()
    }

    /**
     * The class which this entity class extends.
     */
    fun superClass(): KSType {
        val found = decl.superTypes.find {
            val superType = it.resolve().declaration
            (superType is KSClassDeclaration) && (superType.classKind == CLASS)
        }
        return found!!.resolve()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntityClass) return false
        return decl == other.decl
    }

    override fun hashCode(): Int = decl.hashCode()

    /**
     * Obtains the qualified name of the entity class.
     */
    override fun toString(): String = decl.qualifiedName!!.asString()
}
