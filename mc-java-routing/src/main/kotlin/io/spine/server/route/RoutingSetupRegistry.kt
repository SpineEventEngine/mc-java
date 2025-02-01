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

package io.spine.server.route

import io.spine.server.entity.Entity
import java.util.*

private typealias RSetup = RoutingSetup<*, *, *, *, *>

internal object RoutingSetupRegistry {

    private val entries: Set<Entry>

    init {
        val setupInterfaces = setOf(
            CommandRoutingSetup::class,
            EventRoutingSetup::class,
            StateRoutingSetup::class
        )
        val allServices = setupInterfaces
            .map { it.java }
            .flatMap { ServiceLoader.load(it) }
        val grouped = allServices.groupBy { it.entityClass() }

        entries = grouped.map { (cls, setups) -> Entry(cls, setups) }.toSet()
    }

    fun find(
        entityClass: Class<out Entity<*, *>>,
        setupInterface: Class<out RSetup>
    ): RSetup? {
        val entry = entries.find { it.entityClass == entityClass }
        return entry?.find(setupInterface)
    }

    private data class Entry(
        val entityClass: Class<out Entity<*, *>>,
        private val setups: List<RSetup>
    ) {
        init {
            // Check the consistency of grouping.
            setups.forEach { setup ->
                require(setup.entityClass() == entityClass) {
                    val setupClass = setup::class.qualifiedName
                    val servedBySetup = setup.entityClass().simpleName
                    "The `entityClass` (`${entityClass.simpleName}`) of the entry" +
                            " must match the property of the setup (`$setupClass`)." +
                            " Encountered: `$servedBySetup`."
                }
            }
        }

        fun find(
            setupClass: Class<out RSetup>
        ): RSetup? {
            val found = setups.find { setupClass.isAssignableFrom(it.javaClass) }
            return found
        }
    }
}
