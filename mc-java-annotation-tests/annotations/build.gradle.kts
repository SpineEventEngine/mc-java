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

import io.spine.internal.dependency.Spine

plugins {
    id("io.spine.mc-java")
}

dependencies {
    arrayOf(
        Spine.base,
        Spine.pluginTestlib,
        gradleTestKit(),
        project(":mc-java-base")
    ).forEach {
        testImplementation(it)
    }
}

/**
 * ### Enabling remote debugging for ProtoData CLI
 * Since ProtoData launches the code generation in a separate JVM, it is not possible to debug
 * it directly in an IDE. To debug the code generation, perform the following steps:
 *  1. Open the resource file `resources/annotator-plugin-test/build.gradle.kts`.
 *  2. Find the `debugOptions` block.
 *  3. Set the value of the `enabled` property to `true`. Pay attention to the fact that when
 *     enabled, the ProtoData CLI process will be suspended until a debugger is attached.
 *
 * ### Creating a remote debugging configuration in IDEA
 *  Check or create `LaunchProtoData Remote Debug` Run/Debug configuration is available in IDEA:
 *  1. Open the Run/Debug configuration window.
 *  2. Under "Remote JVM Debug" on the left, check if the `LaunchProtoData Remote Debug` is
 *     present, if not, create it via the "+" button at the top left of the dialog.
 *  3. Under "Debugger mode" select "Attach to remote JVM".
 *  4. Under "Host" enter `localhost`.
 *  5. Under "Port" enter `5566`.
 *  6. Under "Use module classpath" selection `io.spine.tools.mc-java-annotations.test`.
 *
 *  ### Starting debugging session
 *  1. Put a breakpoint on `project.executeTask(launchProtoData)` in the `AnnotatorPluginSpec`.
 *  2. Run "Debug 'AnnotatorPluginSpec'" configuration.
 *  3. Once the breakpoint is reached, hit F8 or F9 to run the `launchProtoData` task.
 *     It would take several seconds, depending on the performance of your workstation.
 *     The task will be suspended until the debugger is attached.
 *  4. Once the task is suspended, put a breakpoint in the place of interest of your
 *     [Plugin] or [Renderer] code, which is called by ProtoData.
 *  5. Run the "LaunchProtoData Remote Debug" configuration. You should see a console message
 *     about attaching to a process. If attaching to a process fails, it could mean that
 *     ProtoData CLI has not been started yet. Repeat the attempt in a few seconds.
 */
tasks.findByName("launchProtoData")?.apply { this as JavaExec
    debugOptions {
        enabled.set(false) // Set this option to `true` to enable remote debugging.
        port.set(5566)
        server.set(true)
        suspend.set(true)
        if (enabled.get()) {
            System.err.println("ProtoData configured to run in remote debug mode.")
        }
    }

    doFirst {
        if (debugOptions.enabled.get()) {
            System.err.run {
                val port = debugOptions.port.get()
                println("""
                    ProtoData CLI is launching in remote debug mode. 
                    Waiting for the remote debugger to attach to the port $port...                
                    """.trimIndent())
                flush()
            }
        }
    }
}

tasks.test {
    dependsOn(project(":mc-java-plugin-bundle").tasks.build)
}
