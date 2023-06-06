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
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import io.spine.code.proto.ScalarType;
import io.spine.protodata.Field;
import io.spine.tools.mc.java.CodegenContext;

import java.util.AbstractMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static io.spine.protodata.Ast.isMap;
import static io.spine.tools.mc.java.field.Accessor.prefix;
import static io.spine.tools.mc.java.field.Accessor.prefixAndPostfix;
import static io.spine.tools.mc.java.field.SingularFieldType.constructTypeNameFor;
import static io.spine.tools.mc.java.field.StandardAccessor.clear;
import static io.spine.tools.mc.java.field.StandardAccessor.get;
import static io.spine.tools.mc.java.field.StandardAccessor.getCount;
import static io.spine.tools.mc.java.field.StandardAccessor.getMap;
import static io.spine.tools.mc.java.field.StandardAccessor.put;
import static io.spine.tools.mc.java.field.StandardAccessor.putAll;
import static io.spine.tools.mc.java.field.StandardAccessor.remove;
import static java.util.Objects.requireNonNull;

/**
 * Represents map {@linkplain FieldType field type}.
 */
@Immutable
final class MapFieldType implements FieldType {

    private static final String GET = "get";

    private static final ImmutableSet<Accessor> ACCESSORS = ImmutableSet.of(
            get(),
            getCount(),
            getMap(),
            prefixAndPostfix(GET, "OrDefault"),
            prefixAndPostfix(GET, "OrThrow"),
            prefix("contains"),
            clear(),
            put(),
            remove(),
            putAll()
    );

    @SuppressWarnings("Immutable") // effectively
    private final TypeName typeName;

    /**
     * Constructs the new instance based on the key and the value type names.
     */
    MapFieldType(Field field, CodegenContext context) {
        var entryTypeNames = entryTypeNames(field, context);
        var keyType = boxIfPrimitive(entryTypeNames.getKey());
        var valueType = boxIfPrimitive(entryTypeNames.getValue());
        this.typeName = ParameterizedTypeName.get(ClassName.get(Map.class), keyType, valueType);
    }

    @Override
    public TypeName name() {
        return typeName;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns "putAll" setter prefix,
     * used to initialize a map field using a protobuf message builder.
     *
     * <p>Call should be like `builder.putAllFieldName({@link Map})`.
     */
    @Override
    public Accessor primarySetter() {
        return putAll();
    }

    @Override
    public ImmutableSet<Accessor> accessors() {
        return ACCESSORS;
    }

    private static TypeName boxIfPrimitive(TypeName typeName) {
        if (typeName.isPrimitive()) {
            return typeName.box();
        }
        return typeName;
    }

    @Override
    public String toString() {
        return typeName.toString();
    }

    /**
     * Returns the key and the value type names for the map field
     * based on the passed nested types.
     */
    private static Map.Entry<TypeName, TypeName>
    entryTypeNames(Field mapField, CodegenContext context) {
        checkArgument(isMap(mapField));
        var descrType = requireNonNull(
                TypeConverter.INSTANCE.reverse()
                                      .convert(mapField.getMap().getKeyType())
        );
        var scalarTypeName = ScalarType.javaTypeName(descrType.toProto());
        var keyTypeName = constructTypeNameFor(scalarTypeName);
        var valueTypeName = typeNameOf(mapField, context);
        return new AbstractMap.SimpleEntry<>(keyTypeName, valueTypeName);
    }

    private static TypeName typeNameOf(Field field, CodegenContext context) {
        return FieldType.of(field, context).name();
    }
}
