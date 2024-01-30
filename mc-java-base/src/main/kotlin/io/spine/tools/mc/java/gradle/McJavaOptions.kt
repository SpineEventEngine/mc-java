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
package io.spine.tools.mc.java.gradle

import groovy.lang.Closure
import io.spine.string.Indent.Companion.DEFAULT_JAVA_INDENT_SIZE
import io.spine.tools.java.fs.DefaultJavaPaths
import io.spine.tools.mc.java.gradle.codegen.CodegenOptionsConfig
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

/**
 * A configuration for the Spine Model Compiler for Java.
 */
public abstract class McJavaOptions {

    @get:Nested
    public abstract val annotation: AnnotationSettings

    /**
     * Code generation configuration.
     */
    @JvmField
    public var codegen: CodegenOptionsConfig? = null
    
    /**
     * The indent for the generated code in the validating builders.
     */
    public abstract val indent: Property<Int>

    /**
     * The absolute paths to directories to delete on the `preClean` task.
     */
    @JvmField
    public var tempArtifactDirs: List<String> = ArrayList()

    private lateinit var project: Project

    init {
        @Suppress("LeakingThis")
        indent.convention(DEFAULT_JAVA_INDENT_SIZE)
    }

    /**
     * Injects the dependency to the given project.
     */
    public fun injectProject(project: Project) {
        this.codegen = CodegenOptionsConfig(project)
    }

    public fun annotation(action: Action<AnnotationSettings>) {
        action.execute(annotation)
    }

    /**
     * Applies the given action for code generation options.
     */
    public fun codegen(action: Action<CodegenOptionsConfig>) {
        action.execute(codegen!!)
    }

    @Suppress("unused")
    public // Configures `generateAnnotations` closure.
    fun generateAnnotations(closure: Closure<*>) {
        project.configure(annotation, closure)
    }

    @Suppress("unused")
    public // Configures `generateAnnotations` closure.
    fun generateAnnotations(action: Action<in AnnotationSettings>) {
        action.execute(annotation)
    }

    public companion object {

        /**
         * The name of the extension, as it appears in a Gradle build script.
         */
        public const val NAME: String = "java"

        /**
         * Obtains the extension name of the plugin.
         */
        @JvmStatic
        public fun name(): String {
            return NAME
        }

        @JvmStatic
        public fun def(project: Project): DefaultJavaPaths {
            return DefaultJavaPaths.at(project.projectDir)
        }
    }
}
