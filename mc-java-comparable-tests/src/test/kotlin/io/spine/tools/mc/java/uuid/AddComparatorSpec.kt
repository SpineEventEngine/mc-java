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
    inner class `generate comparator` {

        @Test
        fun `for primitives and enums`() {
            val (cls, _) = generateCode("Account")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            val expected = "private static final Comparator<Account> comparator = " +
                    "Comparator.comparing(Account::getActualData)" +
                    ".thenComparing(Account::getStatus)" +
                    ".thenComparing(Account::getTaxNumber)" +
                    ".thenComparing(Account::getName);"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }

        @Test
        fun `for other comparable messages and nested fields`() {
            val (cls, _) = generateCode("Citizen")
            val checkSuper = false
            val field = cls.findFieldByName("comparator", checkSuper)
            val expected = "private static final Comparator<Citizen> comparator = " +
                    "Comparator.comparing((citizen) -> citizen.getResidence().getRegion())" +
                    ".thenComparing((citizen) -> citizen.getResidence().getCity())" +
                    ".thenComparing(Citizen::getPassport);"
            field.shouldNotBeNull()
            field.text shouldContain expected
        }
    }
}
