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
package io.spine.tools.mc.java.annotation.mark

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.spine.annotation.Internal
import io.spine.code.java.ClassName
import io.spine.tools.mc.java.annotation.given.FakeAnnotator
import io.spine.tools.mc.java.annotation.mark.ModuleAnnotatorSpec.Companion.ANNOTATION
import org.checkerframework.checker.regex.qual.Regex
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ModuleAnnotator` should annotate by")
internal class ModuleAnnotatorSpec {

    companion object {
        internal val ANNOTATION = ClassName.of(Internal::class.java)
        internal val OPTION = ApiOption.internal()
    }

    @Test
    fun `Protobuf option`() {
        checkAnnotateByOption(OPTION)
    }

    @Test
    fun `Protobuf option which does not support fields`() {
        checkAnnotateByOption(ApiOption.spi())
    }

    @Test
    fun `class name pattern`() {
        val classNamePattern: @Regex String = ".+OrBuilder"
        val factory = FakeAnnotator.Factory()
        val annotator = moduleAnnotator {
            internalPatterns = setOf<@Regex String?>(classNamePattern)
            annotatorFactory = factory
            internalAnnotation = ANNOTATION
        }

        annotator.annotate()

        factory.annotationName shouldBe ANNOTATION
        factory.classNamePattern shouldBe ClassNamePattern.compile(classNamePattern)
   }

    @Test
    fun `method name pattern`() {
        val methodName = "setInternalValue"
        val factory = FakeAnnotator.Factory()
        val annotator = moduleAnnotator {
            internalMethodNames = setOf(methodName)
            annotatorFactory = factory
            internalAnnotation = ANNOTATION
        }

        annotator.annotate()

        factory.annotationName shouldBe ANNOTATION
        factory.methodPatterns shouldContainExactly listOf(MethodPattern.exactly(methodName))
    }
}

private fun checkAnnotateByOption(option: ApiOption) {
    val factory = FakeAnnotator.Factory()
    val optionJob = ModuleAnnotator.translate(option).`as`(ANNOTATION)
    val annotator = moduleAnnotator{
        add(optionJob)
        annotatorFactory = factory
        internalAnnotation = ANNOTATION
    }

    annotator.annotate()

    factory.annotationName shouldBe ANNOTATION
    factory.option shouldBe option
}
