/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import io.kotest.matchers.string.shouldContain
import io.spine.tools.mc.java.comparable.given.BytesProhibited
import io.spine.tools.mc.java.comparable.given.MapsProhibited
import io.spine.tools.mc.java.comparable.given.NestedBytesProhibited
import io.spine.tools.mc.java.comparable.given.NestedMapsProhibited
import io.spine.tools.mc.java.comparable.given.NestedNonComparableProhibited
import io.spine.tools.mc.java.comparable.given.NestedNonExistingProhibited
import io.spine.tools.mc.java.comparable.given.NestedOneOfProhibited
import io.spine.tools.mc.java.comparable.given.NestedRepeatedProhibited
import io.spine.tools.mc.java.comparable.given.NoCompareByOption
import io.spine.tools.mc.java.comparable.given.NonComparableProhibited
import io.spine.tools.mc.java.comparable.given.NonExistingProhibited
import io.spine.tools.mc.java.comparable.given.OneOfProhibited
import io.spine.tools.mc.java.comparable.given.RepeatedProhibited
import java.nio.file.Path
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir


@DisplayName("`AddComparator` should report an error if '(compare_by)' option refers to a")
internal class AddComparatorErrorSpec {

    /**
     * The part of the error message instructing the user to read the docs.
     */
    private val callToAction = "Please see the `(compare_by)` option documentation for details."

    private lateinit var projectDir: Path

    @BeforeEach
    fun createProjectDir(@TempDir dir: Path) {
        projectDir = dir
    }

    @Test
    fun `non comparable message`() {
        val error = compile<NonComparableProhibited>(projectDir)
        error.message.let {
            // The field path.
            it shouldContain "`id`"
            // The type of the message declaring the field.
            it shouldContain NonComparableProhibited.getDescriptor().name
            // The type of the field.
            it shouldContain NoCompareByOption.getDescriptor().name
            // The call to action.
            it shouldContain callToAction
        }
    }

    @Test
    fun `'bytes' field`() {
        val error = compile<BytesProhibited>(projectDir)
        error.message.let {
            // The field path.
            it shouldContain "`data`"
            // The declaring message.
            it shouldContain BytesProhibited.getDescriptor().name
            // The type of the field.
            it shouldContain "`bytes`"
        }
    }

    @Nested
    inner class
    `not generate comparator` {

        @Test
        fun `with a bytes field`() = assertNoComparator<BytesProhibited>()

        @Test
        fun `with a repeated field`() = assertNoComparator<RepeatedProhibited>()

        @Test
        fun `with a map field`() = assertNoComparator<MapsProhibited>()

        @Test
        fun `with a non-existing field`() = assertNoComparator<NonExistingProhibited>()

        @Test
        fun `with a oneof field`() = assertNoComparator<OneOfProhibited>()
    }

    @Nested
    inner class
    `not generate comparator with nested` {

        @Test
        fun `non-comparable field`() = assertNoComparator<NestedNonComparableProhibited>()

        @Test
        fun `bytes field`() = assertNoComparator<NestedBytesProhibited>()

        @Test
        fun `repeated field`() = assertNoComparator<NestedRepeatedProhibited>()

        @Test
        fun `map field`() = assertNoComparator<NestedMapsProhibited>()

        @Test
        fun `non-existing field`() = assertNoComparator<NestedNonExistingProhibited>()

        @Test
        fun `oneof field`() = assertNoComparator<NestedOneOfProhibited>()
    }
}
