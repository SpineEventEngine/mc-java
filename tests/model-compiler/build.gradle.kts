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

plugins {
    java
    id("io.spine.mc-java")
}

dependencies {
    protoData(project(":factories"))
    testImplementation(project(":factories"))
}

modelCompiler {
    java {
        codegen {
            // Turn off validation codegen during the transition to the new ProtoData API.
            validation().enabled.set(false)

            val methodFactory = "io.spine.tools.mc.java.protoc.TestMethodFactory"
            val nestedClassAction = "io.spine.tools.mc.java.mgroup.given.NestClassAction"

            forMessages(by().suffix("documents.proto")) {
                useAction("io.spine.tools.mc.java.mgroup.given.ImplementDocumentMessage")
            }

            forMessages(by().prefix("spine/tools/mc/java/protoc/prefix_generation")) {
                generateMethodsWith(methodFactory)
                useAction("io.spine.tools.mc.java.mgroup.given.ImplementPrefixedMessage")
                useAction(nestedClassAction)
            }

            forMessages(by().suffix("suffix_generation_test.proto")) {
                generateMethodsWith(methodFactory)
                //generateNestedClassesWith(nestedClassFactory)
                useAction("io.spine.tools.mc.java.mgroup.given.ImplementSuffixedMessage")
                useAction(nestedClassAction)
            }

            forMessages(by().regex(".*regex.*test.*")) {
                generateMethodsWith(methodFactory)
                //generateNestedClassesWith(nestedClassFactory)
                useAction("io.spine.tools.mc.java.mgroup.given.ImplementRegexMessage")
                useAction(nestedClassAction)
            }

            forMessages(by().regex(".*multi.*factory.*test.*")) {
                generateMethodsWith(methodFactory)
            }
        }
    }
}

tasks.findByName("launchTestProtoData")?.apply { this as JavaExec
    debugOptions {
        enabled.set(false) // Set this option to `true` to enable remote debugging.
        port.set(5566)
        server.set(true)
        suspend.set(true)
    }
}
