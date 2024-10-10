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
import io.spine.testing.logging.mute.MuteLogging
import io.spine.tools.mc.java.comparable.action.AddComparator
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("MaxLineLength") // To keep long `.thenComparing()` closures intact.
@MuteLogging // Negative test cases pollute logging, but we can't suppress only for them because
             // codegen runs once for all messages during the execution of the first test case.
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
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing($message::getActualData)" +
                    ".thenComparing($message::getStatus)" +
                    ".thenComparing($message::getTaxNumber)" +
                    ".thenComparing($message::getName);"
            assertComparator(message, expected)
        }

        @Test
        fun `comparable messages`() {
            val message = "Citizen"
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing($message::getPassport);"
            assertComparator(message, expected)
        }

        @Test
        fun `well-known 'Timestamp' and 'Duration'`() {
            val message = "WithTimestampAndDuration"
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing($message::getTimestamp, $TIMESTAMP_COMPARATOR)" +
                    ".thenComparing($message::getDuration, $DURATION_COMPARATOR);"
            assertComparator(message, expected)
        }

        @Test
        fun `well-known value messages`() {
            val message = "WithValues"
            val instance = message.lowerCamelCase()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing(($message $instance) -> $instance.getBool().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getDouble().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getFloat().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getInt32().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getInt64().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getUint32().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getUint64().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getString().getValue());"
            assertComparator(message, expected)
        }

        @Test
        fun `reversed comparison`() {
            val message = "Debtor"
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing($message::getSum)" +
                    ".thenComparing($message::getName)" +
                    ".reversed();"
            assertComparator(message, expected)
        }
    }

    @Nested inner class
    `generate comparator with nested` {

        @Test
        fun `primitives, enums and comparable messages`() {
            val message = "Traveler"
            val instance = message.lowerCamelCase()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing(($message $instance) -> $instance.getResidence().getRegion())" +
                    ".thenComparing(($message $instance) -> $instance.getResidence().getAddress().getIsActual())" +
                    ".thenComparing(($message $instance) -> $instance.getResidence().getAddress().getCity())" +
                    ".thenComparing(($message $instance) -> $instance.getResidence().getName().getStructure())" +
                    ".thenComparing(($message $instance) -> $instance.getResidence().getName());"
            assertComparator(message, expected)
        }

        @Test
        fun `well-known 'Timestamp' and 'Duration'`() {
            val message = "NestedTimestampAndDuration"
            val instance = message.lowerCamelCase()
            val expected =
                "private static final java.util.Comparator<$message> comparator = " +
                        "java.util.Comparator.comparing(($message $instance) -> $instance.getNested().getTimestamp(), $TIMESTAMP_COMPARATOR)" +
                        ".thenComparing(($message $instance) -> $instance.getNested().getDuration(), $DURATION_COMPARATOR);"
            assertComparator(message, expected)
        }

        @Test
        fun `well-known value messages`() {
            val message = "NestedValues"
            val instance = message.lowerCamelCase()
            val expected = "private static final java.util.Comparator<$message> comparator = " +
                    "java.util.Comparator.comparing(($message $instance) -> $instance.getNested().getBool().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getDouble().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getFloat().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getInt32().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getInt64().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getUint32().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getUint64().getValue())" +
                    ".thenComparing(($message $instance) -> $instance.getNested().getString().getValue());"
            assertComparator(message, expected)
        }
    }

    @Nested inner class
    `not generate comparator` {

        @Test
        fun `without the corresponding option`() = assertNoComparator("NoCompareByOption")

        @Test
        fun `with a non-comparable field`() = assertNoComparator("NonComparableProhibited")

        @Test
        fun `with a bytes field`() = assertNoComparator("BytesProhibited")

        @Test
        fun `with a repeated field`() = assertNoComparator("RepeatedProhibited")

        @Test
        fun `with a map field`() = assertNoComparator("MapsProhibited")

        @Test
        fun `with a non-existing field`() = assertNoComparator("NonExistingProhibited")
    }

    @Nested inner class
    `not generate comparator with nested` {

        @Test
        fun `non-comparable field`() = assertNoComparator("NestedNonComparableProhibited")

        @Test
        fun `bytes field`() = assertNoComparator("NestedBytesProhibited")

        @Test
        fun `repeated field`() = assertNoComparator("NestedRepeatedProhibited")

        @Test
        fun `map field`() = assertNoComparator("NestedMapsProhibited")

        @Test
        fun `non-existing field`() = assertNoComparator("NestedNonExistingProhibited")
    }

    /**
     * Asserts that the given [message] has the [expected] comparator.
     */
    private fun assertComparator(message: String, expected: String) {
        val psiClass = generatedCodeOf(message)
        val field = psiClass.findComparatorField()
        field.shouldNotBeNull()
        field.text shouldContain expected
    }

    /**
     * Asserts that the given [message] doesn't have a comparator.
     */
    private fun assertNoComparator(message: String) {
        val psiClass = generatedCodeOf(message)
        val field = psiClass.findComparatorField()
        field.shouldBeNull()
    }
}

private fun PsiClass.findComparatorField(): PsiField? =
    fields.firstOrNull { it.name == "comparator" }

private inline fun <reified T> fromRegistry() =
    "io.spine.compare.ComparatorRegistry.get(${T::class.java.canonicalName}.class)"
