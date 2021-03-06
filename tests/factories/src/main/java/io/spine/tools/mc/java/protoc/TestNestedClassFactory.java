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

package io.spine.tools.mc.java.protoc;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.spine.type.MessageType;
import io.spine.tools.java.code.NestedClass;
import io.spine.tools.java.code.NestedClassFactory;

import java.util.List;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * A test-only implementation of a {@link NestedClassFactory}.
 */
@SuppressWarnings("unused") // Used through an external class loader in Model Compiler tests.
@Immutable
public final class TestNestedClassFactory implements NestedClassFactory {

    @Override
    public List<NestedClass> generateClassesFor(MessageType messageType) {
        var messageClassName = ClassName.get("", messageType.simpleJavaClassName().value());
        var ownClass = MethodSpec.methodBuilder("messageClass")
                .addModifiers(PUBLIC, STATIC)
                .returns(Class.class)
                .addStatement("return $T.class", messageClassName)
                .addJavadoc("Returns the message class for test purposes.")
                .build();
        var spec = TypeSpec.classBuilder("SomeNestedClass")
                .addModifiers(PUBLIC, STATIC, FINAL)
                .addMethod(ownClass)
                .build();
        var generatedClass = new NestedClass(spec.toString());
        return ImmutableList.of(generatedClass);
    }
}
