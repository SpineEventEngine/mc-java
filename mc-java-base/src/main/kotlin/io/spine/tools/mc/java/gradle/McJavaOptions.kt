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
package io.spine.tools.mc.java.gradle;

import com.google.common.collect.ImmutableSet;
import groovy.lang.Closure;
import io.spine.logging.Logger;
import io.spine.logging.LoggingFactory;
import io.spine.tools.code.Indent;
import io.spine.tools.java.fs.DefaultJavaPaths;
import io.spine.tools.mc.java.gradle.codegen.CodegenOptionsConfig;
import org.gradle.api.Action;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.tools.mc.java.gradle.Projects.getMcJava;
import static java.lang.String.format;

/**
 * A configuration for the Spine Model Compiler for Java.
 */
@SuppressWarnings({
        "PublicField", "WeakerAccess" /* Expose fields as a Gradle extension */,
        "ClassWithTooManyMethods" /* The methods are needed for handing default values. */,
        "ClassWithTooManyFields", "PMD.TooManyFields" /* Gradle extensions are flat like this. */,
        "RedundantSuppression" /* "ClassWithTooManyFields" is sometimes not recognized by IDEA. */
})
public class McJavaOptions {

    private static final Logger<?> logger = LoggingFactory.forEnclosingClass();

    /**
     * The name of the extension, as it appears in a Gradle build script.
     */
    static final String NAME = "java";

    /**
     * The indent for the generated code in the validating builders.
     */
    public Indent indent = Indent.of4();

    /**
     * The absolute paths to directories to delete on the {@code preClean} task.
     */
    public List<String> tempArtifactDirs = new ArrayList<>();

    public final CodeGenAnnotations generateAnnotations = new CodeGenAnnotations();

    /**
     * Code generation configuration.
     *
     * @see #codegen(Action)
     */
    public CodegenOptionsConfig codegen;

    public List<String> internalClassPatterns = new ArrayList<>();

    public List<String> internalMethodNames = new ArrayList<>();

    private Project project;

    /**
     * Injects the dependency to the given project.
     */
    public void injectProject(Project project) {
        this.project = checkNotNull(project);
        this.codegen = new CodegenOptionsConfig(project);
    }

    /**
     * Obtains the extension name of the plugin.
     */
    public static String name() {
        return NAME;
    }

    /**
     * Configures the Model Compilation code generation by applying the given action.
     */
    public void codegen(Action<CodegenOptionsConfig> action) {
        action.execute(codegen);
    }

    static DefaultJavaPaths def(Project project) {
        return DefaultJavaPaths.at(project.getProjectDir());
    }

    public static Indent getIndent(Project project) {
        var result = getMcJava(project).indent;
        logger.atDebug().log(() -> format("The current indent is %d.", result.size()));
        return result;
    }

    @SuppressWarnings("unused")
    public void setIndent(int indent) {
        this.indent = Indent.of(indent);
        logger.atDebug().log(() -> format("Indent has been set to %d.", indent));
    }

    @SuppressWarnings("unused") // Configures `generateAnnotations` closure.
    public void generateAnnotations(Closure<?> closure) {
        project.configure(generateAnnotations, closure);
    }

    @SuppressWarnings("unused") // Configures `generateAnnotations` closure.
    public void generateAnnotations(Action<? super CodeGenAnnotations> action) {
        action.execute(generateAnnotations);
    }

    public static CodeGenAnnotations getCodeGenAnnotations(Project project) {
        var annotations = getMcJava(project).generateAnnotations;
        return annotations;
    }

    public static ImmutableSet<String> getInternalClassPatterns(Project project) {
        var patterns = getMcJava(project).internalClassPatterns;
        return ImmutableSet.copyOf(patterns);
    }

    public static ImmutableSet<String> getInternalMethodNames(Project project) {
        var patterns = getMcJava(project).internalMethodNames;
        return ImmutableSet.copyOf(patterns);
    }
}
