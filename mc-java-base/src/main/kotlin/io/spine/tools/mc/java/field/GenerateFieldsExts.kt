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

package io.spine.tools.mc.java.field

import io.spine.protodata.java.ClassName
import io.spine.tools.java.code.JavaClassName
import io.spine.tools.mc.java.settings.GenerateFields
import io.spine.type.shortDebugString

/**
 * Returns the value of the [superclass][GenerateFields.getSuperclass] property as [ClassName].
 *
 * @throws IllegalStateException
 *          if the [superclass][GenerateFields.getSuperclass] is not populated.
 */
@Suppress("SwallowedException") // Intended.
public val GenerateFields.superClassName: ClassName
    get() {
        check(hasSuperclass()) {
            val clsName = GenerateFields::class.java.canonicalName
            val debugStr = shortDebugString()
            "Unable to obtain a field class supertype from this `$clsName` instance: `$debugStr`."
        }
        try {
            // Try finding a class by its name.
            // This is preferred because it covers tricky cases with nested classes.
            // This may not work if the class is not yet available because it's in the same
            // module with the generated code. When so, we'd get `ClassNotFoundException`.
            val cls = Class.forName(superclass.canonical)
            return ClassName(cls)
        } catch (e: ClassNotFoundException) {
            // Guess the package and class names by parsing.
            return superclass.toClassName()
        }
    }

/**
 * Converts this [JavaClassName] to [ClassName] by parsing its value.
 *
 * The function assumes that package names start with a lowercase letter, and
 * class names start with an uppercase letter.
 */
private fun JavaClassName.toClassName(): ClassName {
    val packageSeparator = "."
    val items = canonical.split(packageSeparator)
    val packageName = items.filter { it[0].isLowerCase() }.joinToString(packageSeparator)
    val simpleNames = items.filter { it[0].isUpperCase() }
    return ClassName(packageName, simpleNames)
}
