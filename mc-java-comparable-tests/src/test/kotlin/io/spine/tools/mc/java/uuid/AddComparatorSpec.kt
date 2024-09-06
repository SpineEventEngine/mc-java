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

package io.spine.tools.mc.java.uuid

import com.google.protobuf.Empty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.spine.tools.mc.java.comparable.action.AddComparator
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`AddComparator` should")
internal class AddComparatorSpec {

    companion object : ComparablePluginTestSetup(
        actionClass = AddComparator::class.java,
        parameter = Empty.getDefaultInstance()
    )

    @Nested
    inner class
    `generate comparator` {

        @Test
        fun `with primitives and enums`() {
            val (cls, _) = generateCode("Account")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            val expected = "private static final java.util.Comparator<Account> comparator = " +
                    "java.util.Comparator.comparing(Account::getActualData)" +
                    ".thenComparing(Account::getStatus)" +
                    ".thenComparing(Account::getTaxNumber)" +
                    ".thenComparing(Account::getName);"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `with comparable messages and nested fields`() {
            val (cls, _) = generateCode("Citizen")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            val expected = "private static final java.util.Comparator<Citizen> comparator = " +
                    "java.util.Comparator.comparing((Citizen citizen) -> citizen.getResidence().getRegion())" +
                    ".thenComparing((Citizen citizen) -> citizen.getResidence().getAddress().getIsActual())" +
                    ".thenComparing((Citizen citizen) -> citizen.getResidence().getAddress().getCity())" +
                    ".thenComparing(Citizen::getPassport);"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `with well-known 'Timestamp' and 'Duration'`() {
            val (cls, _) = generateCode("WithTimestampAndDuration")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            val expected = "private static final java.util.Comparator<WithTimestampAndDuration> comparator = " +
                    "java.util.Comparator.comparing(WithTimestampAndDuration::getTimestamp, com.google.protobuf.util.Timestamps.comparator())" +
                    ".thenComparing(WithTimestampAndDuration::getDuration, com.google.protobuf.util.Durations.comparator());"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `with nested well-known 'Timestamp' and 'Duration'`() {
            val (cls, _) = generateCode("NestedTimestampAndDuration")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            val expected = "private static final java.util.Comparator<NestedTimestampAndDuration> comparator = " +
                    "java.util.Comparator.comparing((NestedTimestampAndDuration nestedTimestampAndDuration) -> nestedTimestampAndDuration.getNested().getTimestamp(), com.google.protobuf.util.Timestamps.comparator())" +
                    ".thenComparing((NestedTimestampAndDuration nestedTimestampAndDuration) -> nestedTimestampAndDuration.getNested().getDuration(), com.google.protobuf.util.Durations.comparator());"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `with well-known value messages`() {
            val (cls, _) = generateCode("WithValues")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            val expected = "private static final java.util.Comparator<WithValues> comparator = " +
                    "java.util.Comparator.comparing((WithValues withValues) -> withValues.getBool().getValue())" +
                    ".thenComparing((WithValues withValues) -> withValues.getDouble().getValue())" +
                    ".thenComparing((WithValues withValues) -> withValues.getFloat().getValue())" +
                    ".thenComparing((WithValues withValues) -> withValues.getInt32().getValue())" +
                    ".thenComparing((WithValues withValues) -> withValues.getInt64().getValue())" +
                    ".thenComparing((WithValues withValues) -> withValues.getUint32().getValue())" +
                    ".thenComparing((WithValues withValues) -> withValues.getUint64().getValue())" +
                    ".thenComparing((WithValues withValues) -> withValues.getString().getValue());"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `with nested well-known value messages`() {
            val (cls, _) = generateCode("NestedValues")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            val expected = "private static final java.util.Comparator<NestedValues> comparator = " +
                    "java.util.Comparator.comparing((NestedValues nestedValues) -> nestedValues.getNested().getBool().getValue())" +
                    ".thenComparing((NestedValues nestedValues) -> nestedValues.getNested().getDouble().getValue())" +
                    ".thenComparing((NestedValues nestedValues) -> nestedValues.getNested().getFloat().getValue())" +
                    ".thenComparing((NestedValues nestedValues) -> nestedValues.getNested().getInt32().getValue())" +
                    ".thenComparing((NestedValues nestedValues) -> nestedValues.getNested().getInt64().getValue())" +
                    ".thenComparing((NestedValues nestedValues) -> nestedValues.getNested().getUint32().getValue())" +
                    ".thenComparing((NestedValues nestedValues) -> nestedValues.getNested().getUint64().getValue())" +
                    ".thenComparing((NestedValues nestedValues) -> nestedValues.getNested().getString().getValue());"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }
    }

    @Nested
    inner class
    `not generate comparator for a message` {

        @Test
        fun `without the corresponding option`() {
            val (cls, _) = generateCode("NoCompareByOption")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            field.shouldBeNull()
        }

        @Test
        fun `with a non-comparable field`() {
            val (cls, _) = generateCode("NonComparablesProhibited")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            field.shouldBeNull()
        }

        @Test
        fun `with a bytes field`() {
            val (cls, _) = generateCode("BytesProhibited")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            field.shouldBeNull()
        }

        @Test
        fun `with a repeated field`() {
            val (cls, _) = generateCode("RepeatedProhibited")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            field.shouldBeNull()
        }

        @Test
        fun `with a map field`() {
            val (cls, _) = generateCode("MapsProhibited")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            field.shouldBeNull()
        }

        @Test
        fun `with a non-existing field`() {
            val (cls, _) = generateCode("NonExistingProhibited")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            field.shouldBeNull()
        }
    }

    @Nested
    inner class
    `not generate comparator for a message with nested` {

        @Test
        fun `non-comparable field`() {
            val (cls, _) = generateCode("NestedNonComparablesProhibited")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            field.shouldBeNull()
        }

        @Test
        fun `bytes field`() {
            val (cls, _) = generateCode("NestedBytesProhibited")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            field.shouldBeNull()
        }

        @Test
        fun `repeated field`() {
            val (cls, _) = generateCode("NestedRepeatedProhibited")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            field.shouldBeNull()
        }

        @Test
        fun `map field`() {
            val (cls, _) = generateCode("NestedMapsProhibited")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            field.shouldBeNull()
        }

        @Test
        fun `non-existing field`() {
            val (cls, _) = generateCode("NestedNonExistingProhibited")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            field.shouldBeNull()
        }
    }
}
