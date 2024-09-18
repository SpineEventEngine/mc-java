/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.marker

import io.spine.base.EntityState
import io.spine.option.IsOption
import io.spine.protodata.MessageType
import io.spine.protodata.ProtoFileHeader
import io.spine.protodata.java.JavaTypeName.Companion.PACKAGE_SEPARATOR
import io.spine.protodata.java.javaPackage
import io.spine.tools.mc.java.BaseRenderer
import io.spine.tools.mc.java.ImplementInterface
import io.spine.tools.mc.java.SuperInterface
import org.checkerframework.checker.signature.qual.FullyQualifiedName

internal abstract class MarkerRenderer<V : EntityState<*>> : BaseRenderer<V>() {

    /**
     * Makes the Java class corresponding to this message type implement the given [superInterface].
     */
    protected fun MessageType.implementInterface(superInterface: SuperInterface) {
        val file = sources.javaFileOf(this)
        val action = ImplementInterface(this, file, superInterface, context!!)
        action.render()
    }
}

internal typealias InterfaceName = @FullyQualifiedName String

//TODO:2024-09-18:alexander.yevsyukov: Use ext. ProtoData when new options are available.
internal fun IsOption.qualifiedJavaType(header: ProtoFileHeader): InterfaceName {
    check(javaType.isNotEmpty() && javaType.isNotBlank()) {
        "The value of `java_type` must not be empty or blank. Got: `\"$javaType\"`."
    }
    return if (javaType.isQualified) {
        javaType
    } else {
        "${header.javaPackage()}.$javaType"
    }
}

private val String.isQualified: Boolean
    get() = contains(PACKAGE_SEPARATOR)
