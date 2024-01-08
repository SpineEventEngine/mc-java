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

package io.spine.tools.mc.java.annotation.assertions

import io.spine.string.camelCase
import io.spine.string.contains
import io.spine.test.annotation.Attempt
import io.spine.test.annotation.CustomBeta
import io.spine.test.annotation.Private
import io.spine.test.annotation.ServiceProviderInterface
import java.lang.reflect.AnnotatedElement
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Assertions for the annotator plugin tests.
 */
object Assertions {

    @JvmStatic
    fun assertSpi(element: AnnotatedElement) {
        assertAnnotated(element, ServiceProviderInterface::class.java)
    }

    @JvmStatic
    fun assertNotSpi(element: AnnotatedElement) {
        assertNotAnnotated(element, ServiceProviderInterface::class.java)
    }

    @JvmStatic
    fun assertInternal(element: AnnotatedElement) {
        assertAnnotated(element, Private::class.java)
    }

    @JvmStatic
    fun assertNotInternal(element: AnnotatedElement) {
        assertNotAnnotated(element, Private::class.java)
    }

    @JvmStatic
    fun assertExperimental(element: AnnotatedElement) {
        assertAnnotated(element, Attempt::class.java)
    }

    @JvmStatic
    fun assertNotExperimental(element: AnnotatedElement) {
        assertNotAnnotated(element, Attempt::class.java)
    }

    @JvmStatic
    fun assertBeta(element: AnnotatedElement) {
        assertAnnotated(element, CustomBeta::class.java)
    }

    @JvmStatic
    fun assertNotBeta(element: AnnotatedElement) {
        assertNotAnnotated(element, CustomBeta::class.java)
    }

}

fun assertAnnotated(
    element: AnnotatedElement,
    expected: Class<out Annotation>
) {
    assertTrue(
        element.isAnnotationPresent(expected),
        String.format("%s must be annotated with `%s.`", element, expected.name)
    )
}

fun assertNotAnnotated(
    element: AnnotatedElement,
    notExpected: Class<out Annotation>
) {
    assertFalse(
        element.isAnnotationPresent(notExpected),
        String.format(
            "%s must NOT be annotated with %s.",
            element,
            notExpected.simpleName
        )
    )
}

fun assertAnnotation(
    element: AnnotatedElement,
    expectedAnnotation: Class<out Annotation>,
    present: Boolean
) {
    if (present) {
        assertAnnotated(element, expectedAnnotation)
    } else {
        assertNotAnnotated(element, expectedAnnotation)
    }
}

fun assertAnnotationOfAccessors(
    cls: Class<out Any>,
    fieldName: String,
    expectedAnnotation: Class<out Annotation>,
    present: Boolean
) {
    val camelCase = fieldName.camelCase()

    val accessors = cls.methods.filter { method -> method.name.contains(camelCase) }
    accessors.forEach {
        assertAnnotation(it, expectedAnnotation, present)
    }
}
