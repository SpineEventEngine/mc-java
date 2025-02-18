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

import io.spine.dependency.build.ErrorProne
import io.spine.dependency.lib.AutoService
import io.spine.dependency.local.Base
import io.spine.dependency.local.ModelCompiler
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.ToolBase

dependencies {
    annotationProcessor(AutoService.processor)
    compileOnlyApi(AutoService.annotations)
    compileOnly(gradleApi())

    api(ErrorProne.core)
    ErrorProne.annotations.forEach { api(it) }
    implementation(ErrorProne.GradlePlugin.lib)

    implementation(Base.lib)
    implementation(ToolBase.pluginBase)
    implementation(ModelCompiler.lib)

    testImplementation(ErrorProne.testHelpers)
    testImplementation(gradleKotlinDsl())
    testImplementation(TestLib.lib)
}

/**
 * Adds the `--add-exports` compiler argument which exports the given package from
 * the `jdk.compiler` module to the default unnamed module.
 */
fun CompileOptions.exportsJavacPackage(packageName: String) {
    compilerArgs.add("--add-exports")
    compilerArgs.add("jdk.compiler/$packageName=ALL-UNNAMED")
}

/**
 * Adds the `--add-exports` compiler arguments for all the given `com.sun.tools.javac` subpackages.
 *
 * We need to expose the internal Java compiler API to find potential bugs in code.
 * These compiler arguments are only required at compile time of the `mc-java-checks` module.
 *
 * Users of Error Prone, regardless of using `mc-java-checks`, might need to add compiler and
 * runtime flags of their own.
 * The full list is available from [Error Prone docs](https://errorprone.info/docs/installation).
 */
fun CompileOptions.exportsSunJavacPackages(vararg subpackages: String) {
    subpackages.forEach {
        exportsJavacPackage("com.sun.tools.javac.$it")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.exportsSunJavacPackages(
        "api",
        "code",
        "file",
        "code",
        "util",
        "comp",
        "main",
        "model",
        "parser",
        "processing",
        "tree"
    )
}

tasks.withType<Javadoc>().configureEach {
    enabled = false
}
