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

package io.spine.tools.mc.java.field;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.spine.code.proto.FieldDeclaration;
import io.spine.code.proto.ScalarType;
import io.spine.protodata.Field;
import io.spine.tools.mc.java.CodegenContext;
import io.spine.tools.mc.java.TypeSystem;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.STRING;
import static io.spine.protodata.PrimitiveType.TYPE_STRING;
import static io.spine.tools.mc.java.field.Accessor.prefix;
import static io.spine.tools.mc.java.field.Accessor.prefixAndPostfix;
import static io.spine.tools.mc.java.field.StandardAccessor.clear;
import static io.spine.tools.mc.java.field.StandardAccessor.get;
import static io.spine.tools.mc.java.field.StandardAccessor.set;
import static java.util.Objects.requireNonNull;

/**
 * Represents singular {@linkplain FieldType field type}.
 */
@Immutable
final class SingularFieldType implements FieldType {

    private static final String BYTES = "Bytes";

    private static final ImmutableSet<Accessor> ACCESSORS = ImmutableSet.of(
            prefix("has"),
            get(),
            set(),
            clear()
    );

    private static final ImmutableSet<Accessor> STRING_ACCESSORS = ImmutableSet.of(
            prefixAndPostfix("get", BYTES),
            prefixAndPostfix("set", BYTES)
    );

    @SuppressWarnings("Immutable") // effectively
    private final TypeName typeName;
    private final Field field;

    /**
     * Creates a new instance based on field type name.
     */
    SingularFieldType(Field field, CodegenContext context) {
        var typeSystem = context.getTypeSystem();
        var javaTypeName = typeSystem.javaTypeName(field.getType());

        this.typeName = constructTypeNameFor(javaTypeName);
        this.field = field;
    }

    @Override
    public TypeName name() {
        return typeName;
    }

    @Override
    public ImmutableSet<Accessor> accessors() {
        return (field.getType().getPrimitive() == TYPE_STRING)
             ? ImmutableSet.<Accessor>builder()
                           .addAll(ACCESSORS)
                           .addAll(STRING_ACCESSORS)
                           .build()
             : ACCESSORS;
    }

    /**
     * Returns "set" setter template used to initialize a singular field using a Protobuf message
     * builder.
     *
     * <p>The call should have the following structure: {@code builder.setFieldName(FieldType)}.
     */
    @Override
    public Accessor primarySetter() {
        return set();
    }

    public static TypeName constructTypeNameFor(String name) {
        var boxedScalarPrimitive = PrimitiveType.wrapperFor(name);

        if (boxedScalarPrimitive.isPresent()) {
            var unboxed = TypeName.get(boxedScalarPrimitive.get()).unbox();
            return unboxed;
        }

        // Make a possibly nested class name use the dot notation.
        var dottedName = name.replace('$', '.');
        var result = ClassName.bestGuess(dottedName);
        return result;
    }
}
