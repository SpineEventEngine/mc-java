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

package io.spine.tools.mc.java.validation.gen;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import io.spine.base.FieldPath;
import io.spine.code.proto.FieldContext;
import io.spine.protobuf.TypeConverter;
import io.spine.type.MessageType;
import io.spine.validate.ConstraintViolation;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

/**
 * An {@link Expression} which creates a new {@link ConstraintViolation}.
 */
final class NewViolation implements Expression<ConstraintViolation> {

    private final String message;
    private final ImmutableList<String> params;
    private final @Nullable FieldAccess fieldValue;
    private final MessageType type;
    private final FieldPath field;
    private final @Nullable Expression<? extends Iterable<ConstraintViolation>> nestedViolations;

    private NewViolation(Builder builder) {
        this.message = builder.message;
        this.fieldValue = builder.fieldValue;
        this.type = builder.type;
        this.field = builder.field;
        this.nestedViolations = builder.nestedViolations;
        this.params = ImmutableList.copyOf(builder.params);
    }

    @Override
    public CodeBlock toCode() {
        @SuppressWarnings("DuplicateStringLiteralInspection")
        var builder = CodeBlock.builder()
                .add("$T.newBuilder()", ConstraintViolation.class)
                .add(".setMsgFormat($S)", message)
                .add(".setTypeName($S)", type.name().value());
        addFieldValue(builder);
        addViolations(builder);
        addFieldPath(builder);
        addParams(builder);
        builder.add(".build()");
        return builder.build();
    }

    private void addViolations(CodeBlock.Builder builder) {
        if (nestedViolations != null) {
            builder.add(".addAllViolation($L)", nestedViolations.toCode());
        }
    }

    private void addFieldValue(CodeBlock.Builder builder) {
        if (fieldValue != null) {
            builder.add(".setFieldValue($T.toAny($L))", TypeConverter.class, fieldValue.toCode());
        }
    }

    private void addFieldPath(CodeBlock.Builder builder) {
        if (field.getFieldNameCount() > 0) {
            builder.add(".setFieldPath($T.newBuilder()", FieldPath.class);
            for (var fieldName : field.getFieldNameList()) {
                builder.add(".addFieldName($S)", fieldName);
            }
            builder.add(".build())");
        }
    }

    private void addParams(CodeBlock.Builder builder) {
        for (var param : params) {
            builder.add(".addParam($S)", param);
        }
    }

    @Override
    public String toString() {
        return toCode().toString();
    }

    /**
     * Creates a new builder for the violation in the given field.
     */
    static Builder forField(FieldContext field) {
        checkNotNull(field);
        var declaration = field.targetDeclaration();
        return new Builder()
                .setType(declaration.declaringType())
                .setField(field.fieldPath());
    }

    /**
     * Creates a new builder for a violation in the field of the specified message.
     */
    static Builder forMessage(MessageType type, FieldContext field) {
        checkNotNull(field);
        return new Builder()
                .setType(type)
                .setField(field.fieldPath());
    }

    /**
     * A builder for the {@code ViolationTemplate} instances.
     */
    static final class Builder {

        private String message;
        private @Nullable FieldAccess fieldValue;
        private MessageType type;
        private FieldPath field;
        private @Nullable Expression<? extends Iterable<ConstraintViolation>> nestedViolations;
        private final List<String> params = new ArrayList<>();

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        Builder setMessage(String message) {
            this.message = checkNotNull(message);
            return this;
        }

        Builder setFieldValue(FieldAccess fieldValue) {
            this.fieldValue = checkNotNull(fieldValue);
            return this;
        }

        Builder setType(MessageType type) {
            this.type = checkNotNull(type);
            return this;
        }

        Builder setField(FieldPath field) {
            this.field = checkNotNull(field);
            return this;
        }

        Builder
        setNestedViolations(Expression<? extends Iterable<ConstraintViolation>> nestedViolations) {
            this.nestedViolations = checkNotNull(nestedViolations);
            return this;
        }

        Builder addParam(String... value) {
            params.addAll(asList(value));
            return this;
        }

        /**
         * Creates a new instance.
         */
        NewViolation build() {
            checkNotNull(message);
            checkNotNull(type);
            checkNotNull(field);
            return new NewViolation(this);
        }
    }
}
