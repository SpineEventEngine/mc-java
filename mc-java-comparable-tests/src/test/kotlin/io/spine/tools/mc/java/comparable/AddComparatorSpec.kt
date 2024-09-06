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

package io.spine.tools.mc.java.comparable

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.spine.tools.mc.java.comparable.action.AddComparator
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`AddComparator` should")
internal class AddComparatorSpec {

    companion object : ComparablePluginTestSetup(AddComparator::class)

    @Nested
    inner class
    `generate comparator` {

        @Test
        fun `with primitives and enums`() {
            val message = "Account"
            val psiClass = generatedCodeOf(message)
            val field = psiClass.findComparatorField()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing($message::getActualData)" +
                    ".thenComparing($message::getStatus)" +
                    ".thenComparing($message::getTaxNumber)" +
                    ".thenComparing($message::getName);"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `with comparable messages and nested fields`() {
            val message = "Citizen"
            val instance = message.lowerCased
            val psiClass = generatedCodeOf(message)
            val field = psiClass.findComparatorField()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing(($message $instance) -> $instance.getResidence().getRegion())" +
                    ".thenComparing(($message $instance) -> $instance.getResidence().getAddress().getIsActual())" +
                    ".thenComparing(($message $instance) -> $instance.getResidence().getAddress().getCity())" +
                    ".thenComparing($message::getPassport);"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `with well-known 'Timestamp' and 'Duration'`() {
            val message = "WithTimestampAndDuration"
            val psiClass = generatedCodeOf(message)
            val field = psiClass.findComparatorField()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing($message::getTimestamp, com.google.protobuf.util.Timestamps.comparator())" +
                    ".thenComparing($message::getDuration, com.google.protobuf.util.Durations.comparator());"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `with nested well-known 'Timestamp' and 'Duration'`() {
            val message = "NestedTimestampAndDuration"
            val instance = message.lowerCased
            val psiClass = generatedCodeOf(message)
            val field = psiClass.findComparatorField()
            val expected =
                "private static final java.util.Comparator<$message> comparator = " +
                        "java.util.Comparator.comparing(($message $instance) -> $instance.getNested().getTimestamp(), com.google.protobuf.util.Timestamps.comparator())" +
                        ".thenComparing(($message $instance) -> $instance.getNested().getDuration(), com.google.protobuf.util.Durations.comparator());"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `with well-known value messages`() {
            val message = "WithValues"
            val instance = message.lowerCased
            val psiClass = generatedCodeOf(message)
            val field = psiClass.findComparatorField()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing(($message $instance) -> $instance.getBool().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getDouble().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getFloat().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getInt32().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getInt64().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getUint32().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getUint64().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getString().getValue());"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `with nested well-known value messages`() {
            val message = "NestedValues"
            val instance = message.lowerCased
            val psiClass = generatedCodeOf(message)
            val field = psiClass.findComparatorField()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing(($message $instance) -> $instance.getNested().getBool().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getDouble().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getFloat().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getInt32().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getInt64().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getUint32().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getUint64().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getString().getValue());"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `with reversed comparison`() {
            val message = "Debtor"
            val psiClass = generatedCodeOf(message)
            val field = psiClass.findComparatorField()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing($message::getSum)" +
                    ".thenComparing($message::getName)" +
                    ".thenComparing($message::getTaxNumber)" +
                    ".reversed();"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }
    }

    @Nested
    inner class
    `not generate comparator for a message` {

        @Test
        fun `without the corresponding option`() = testNotGenerateFor("NoCompareByOption")

        @Test
        fun `with a non-comparable field`() = testNotGenerateFor("NonComparablesProhibited")

        @Test
        fun `with a bytes field`() = testNotGenerateFor("BytesProhibited")

        @Test
        fun `with a repeated field`() = testNotGenerateFor("RepeatedProhibited")

        @Test
        fun `with a map field`() = testNotGenerateFor("MapsProhibited")

        @Test
        fun `with a non-existing field`() = testNotGenerateFor("NonExistingProhibited")
    }

    @Nested
    inner class
    `not generate comparator for a message with nested` {

        @Test
        fun `non-comparable field`() = testNotGenerateFor("NestedNonComparablesProhibited")

        @Test
        fun `bytes field`() = testNotGenerateFor("NestedBytesProhibited")

        @Test
        fun `repeated field`() = testNotGenerateFor("NestedRepeatedProhibited")

        @Test
        fun `map field`() = testNotGenerateFor("NestedMapsProhibited")

        @Test
        fun `non-existing field`() = testNotGenerateFor("NestedNonExistingProhibited")
    }

    private fun testNotGenerateFor(message: String) {
        val psiClass = generatedCodeOf(message)
        val field = psiClass.findComparatorField()
        field.shouldBeNull()
    }
}

private fun PsiClass.findComparatorField(): PsiField? =
    fields.firstOrNull { it.name == "comparator" }

private val String.lowerCased
    get() = replaceFirstChar { it.lowercase() }
