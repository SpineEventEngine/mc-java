/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import com.google.common.base.Objects;
import com.google.errorprone.annotations.Immutable;
import io.spine.tools.java.code.field.FieldName;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A template of an accessor method generated by the Protobuf compiler for a Protobuf field in Java.
 *
 * <p>An accessor always has a prefix (e.g. {@code get...}) and may have a postfix
 * (e.g. {@code ...Count}).
 */
@Immutable
public final class Accessor implements Serializable {

    private static final long serialVersionUID = 0L;

    private final String prefix;
    private final String postfix;

    private Accessor(String prefix, String postfix) {
        this.prefix = prefix;
        this.postfix = postfix;
    }

    /**
     * Creates a new template with the given prefix and an empty postfix.
     */
    public static Accessor prefix(String prefix) {
        checkNotNull(prefix);
        return new Accessor(prefix, "");
    }

    /**
     * Creates a new template with the given prefix and postfix.
     */
    public static Accessor prefixAndPostfix(String prefix, String suffix) {
        checkNotNull(prefix);
        checkNotNull(suffix);
        return new Accessor(prefix, suffix);
    }

    /**
     * Formats an accessor method name based on this template and the given field name.
     *
     * @param field
     *         the name of the field to access
     * @return the method name
     */
    public String format(FieldName field) {
        String name = String.format(template(), field.capitalize());
        return name;
    }

    private String template() {
        return prefix + "%s" + postfix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Accessor template = (Accessor) o;
        return Objects.equal(template(), template.template());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(prefix, postfix);
    }

    @Override
    public String toString() {
        return template();
    }
}
