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

import io.spine.internal.dependency.JavaPoet
import io.spine.internal.dependency.JavaX
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Validation

// IMPORTANT: This module is deprecated and will be removed in the future.
// It is no longer used in the production code and is kept for historical purposes
// until we fully migrate to ProtoData-based validation.
// This module is not published and excluded from the dependencies in other modules.
// It is still referenced in `settings.gradle.kts` to keep the project structure
// intact so that IDEA can pick up and index the code.
//

dependencies {
    api(JavaPoet.lib)
    api(Validation.runtime)
    implementation(Spine.toolBase)
    implementation(JavaX.annotations)

    implementation(project(":mc-java-base"))

    testImplementation(Spine.testlib)

    testImplementation(gradleTestKit())
    testImplementation(Spine.pluginBase)
    testImplementation(Spine.pluginTestlib)
}
