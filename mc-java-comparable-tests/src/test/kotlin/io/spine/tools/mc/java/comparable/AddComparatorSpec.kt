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

import com.google.protobuf.Duration
import com.google.protobuf.Timestamp
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.spine.string.lowerCamelCase
import io.spine.tools.mc.java.comparable.action.AddComparator
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("MaxLineLength") // Long `.thenComparing()` closures.
@DisplayName("`AddComparator` should")
internal class AddComparatorSpec {

    companion object : ComparablePluginTestSetup(AddComparator::class) {
        val TIMESTAMP_COMPARATOR = fromRegistry<Timestamp>()
        val DURATION_COMPARATOR = fromRegistry<Duration>()
    }

    @Nested inner class
    `generate comparator with` {

        @Test
        fun `primitives and enums`() {
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
        fun `comparable messages`() {
            val message = "Citizen"
            val psiClass = generatedCodeOf(message)
            val field = psiClass.findComparatorField()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing($message::getPassport);"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `well-known 'Timestamp' and 'Duration'`() {
            val message = "WithTimestampAndDuration"
            val psiClass = generatedCodeOf(message)
            val field = psiClass.findComparatorField()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing($message::getTimestamp, $TIMESTAMP_COMPARATOR)" +
                    ".thenComparing($message::getDuration, $DURATION_COMPARATOR);"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `well-known value messages`() {
            val message = "WithValues"
            val instance = message.lowerCamelCase()
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
        fun `reversed comparison`() {
            val message = "Debtor"
            val psiClass = generatedCodeOf(message)
            val field = psiClass.findComparatorField()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing($message::getSum)" +
                    ".thenComparing($message::getName)" +
                    ".reversed();"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }
    }

    @Nested inner class
    `generate comparator with nested` {

        @Test
        fun `primitives, enums and comparable messages`() {
            val message = "Traveler"
            val instance = message.lowerCamelCase()
            val psiClass = generatedCodeOf(message)
            val field = psiClass.findComparatorField()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing(($message $instance) -> $instance.getResidence().getRegion())" +
                    ".thenComparing(($message $instance) -> $instance.getResidence().getAddress().getIsActual())" +
                    ".thenComparing(($message $instance) -> $instance.getResidence().getAddress().getCity())" +
                    ".thenComparing(($message $instance) -> $instance.getResidence().getName().getStructure())" +
                    ".thenComparing(($message $instance) -> $instance.getResidence().getName());"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `well-known 'Timestamp' and 'Duration'`() {
            val message = "NestedTimestampAndDuration"
            val instance = message.lowerCamelCase()
            val psiClass = generatedCodeOf(message)
            val field = psiClass.findComparatorField()
            val expected =
                "private static final java.util.Comparator<$message> comparator = " +
                        "java.util.Comparator.comparing(($message $instance) -> $instance.getNested().getTimestamp(), $TIMESTAMP_COMPARATOR)" +
                        ".thenComparing(($message $instance) -> $instance.getNested().getDuration(), $DURATION_COMPARATOR);"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `well-known value messages`() {
            val message = "NestedValues"
            val instance = message.lowerCamelCase()
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
    }

    @Nested inner class
    `not generate comparator` {

        @Test
        fun `without the corresponding option`() = testNotGeneratesFor("NoCompareByOption")

        @Test
        fun `with a non-comparable field`() = testNotGeneratesFor("NonComparableProhibited")

        @Test
        fun `with a bytes field`() = testNotGeneratesFor("BytesProhibited")

        @Test
        fun `with a repeated field`() = testNotGeneratesFor("RepeatedProhibited")

        @Test
        fun `with a map field`() = testNotGeneratesFor("MapsProhibited")

        @Test
        fun `with a non-existing field`() = testNotGeneratesFor("NonExistingProhibited")
    }

    @Nested inner class
    `not generate comparator with nested` {

        @Test
        fun `non-comparable field`() = testNotGeneratesFor("NestedNonComparableProhibited")

        @Test
        fun `bytes field`() = testNotGeneratesFor("NestedBytesProhibited")

        @Test
        fun `repeated field`() = testNotGeneratesFor("NestedRepeatedProhibited")

        @Test
        fun `map field`() = testNotGeneratesFor("NestedMapsProhibited")

        @Test
        fun `non-existing field`() = testNotGeneratesFor("NestedNonExistingProhibited")
    }

    private fun testNotGeneratesFor(message: String) {
        val psiClass = generatedCodeOf(message)
        val field = psiClass.findComparatorField()
        field.shouldBeNull()
    }
}

private fun PsiClass.findComparatorField(): PsiField? =
    fields.firstOrNull { it.name == "comparator" }

private inline fun <reified T> fromRegistry() =
    "io.spine.compare.ComparatorRegistry.INSTANCE.get(${T::class.java.canonicalName}.class)"
