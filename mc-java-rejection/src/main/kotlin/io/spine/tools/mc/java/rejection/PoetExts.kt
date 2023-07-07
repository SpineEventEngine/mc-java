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

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec

/**
 * Creates a new [MethodSpec] with via customizing it using
 * the given [action] on the builder.
 *
 * @param name
 *         The name of the method.
 * @param action
 *         The action to be performed on the [MethodSpec.Builder].
 */
internal fun methodSpec(name: String, action: MethodSpec.Builder.() -> Unit): MethodSpec {
    val builder = MethodSpec.methodBuilder(name)
    action(builder)
    return builder.build()
}

/**
 * Creates a [TypeSpec] for a class with the given name. The class is customized via the given
 * builder action.
 *
 * @param name
 *         The name of the class.
 * @param action
 *         The action on the builder to create the class.
 */
internal fun classSpec(name: String, action: TypeSpec.Builder.() -> Unit): TypeSpec {
    val builder = TypeSpec.classBuilder(name)
    action(builder)
    return builder.build()
}

/**
 * Creates a [MethodSpec] for a parameterless constructor with the given action on the builder.
 */
internal fun constructorSpec(action: MethodSpec.Builder.() -> Unit): MethodSpec {
    val builder = MethodSpec.constructorBuilder()
    action(builder)
    return builder.build()
}

/**
 * Creates a [CodeBlock] with the given action on the builder.
 */
internal fun codeBlock(action: CodeBlock.Builder.() -> Unit): CodeBlock {
    val builder = CodeBlock.builder()
    action(builder)
    return builder.build()
}
