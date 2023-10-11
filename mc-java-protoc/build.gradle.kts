/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import io.spine.internal.dependency.JavaPoet
import io.spine.internal.dependency.JavaX
import io.spine.internal.dependency.Spine

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())

    implementation(Spine.toolBase)
    implementation(project(":mc-java-base"))
    implementation(JavaPoet.lib)
    implementation(JavaX.annotations)

    testImplementation(gradleTestKit())
    testImplementation(Spine.base)
    testImplementation(Spine.pluginTestlib)
}

tasks.jar {
    //TODO:2021-08-01:alexander.yevsyukov: Replace the below dependencies with output of `jar` tasks
    // instead. See:
    //   https://discuss.gradle.org/t/gradle-7-0-seems-to-take-an-overzealous-approach-to-inter-task-dependencies/39656/4
    //   https://docs.gradle.org/current/userguide/userguide_single.html?&_ga=2.136886832.1455643218.1627825963-149591519.1626535262#sec:link_output_dir_to_input_files
    //
    dependsOn(
        project(":mc-java-base").tasks.jar
    )

    exclude(
        // See https://stackoverflow.com/questions/35704403/what-are-the-eclipsef-rsa-and-eclipsef-sf-in-a-java-jar-file
        "META-INF/*.RSA",
        "META-INF/*.SF",
        "META-INF/*.DSA",

        // Manually exclude the given runtime dependencies until we specify our dependencies in
        // a more precise way. These dependencies are transitive, e.g. from Gradle and are
        // available to McJava in runtime.
        "ant_tasks/",
        "bsh/",
        "checkstyle*",
        "images*",
        "kotlin/**",
        "mozilla*",
        "com/amazonaws/**",
        "com/github/javaparser",
        "com/google/api/**",
        "com/google/cloud/**",
        "images/ant*.*",
        "jcifs/", // Unused *.idl files.
        "com/sun/xml/**", // Strange HTML files.
        "net/rubygrapefruit/**",
        "org/apache/**",
        "org/bouncycastle/**",
        "org/checkerframework/**",
        "org/codehaus/**",
        "org/fusesource/**",
        "org/glassfish/**",
        "org/gradle/**",
        "org/intellij/**",
        "org/jboss/**",
        "org/jetbrains/**",
        "org/joda/**",
        "org/slf4j/**",
        "org/groovy/**",
        "OSGI-*/", // from Eclipse.
        "groovy*/**",
        ".api_description", // from Eclipse.
        ".options", // from Eclipse.
        "api-mapping.txt", // from Gradle.
        "about.html", // from Eclipse.
        "default-imports.txt", // from Gradle.
        "jdtCompilerAdapter.jar", // from Gradle.
        "release-features.txt", // from Gradle.
        "systembundle.properties", // from Eclipse.
        "storage.v1.json", // from Google Cloud.
        "CDC-*.profile", // from Eclipse.
        "junit/**",
        "testng*.*", // from TestNG.
        "mozilla/**",
        "gradle-*",
        "J2SE-*",
        "JavaSE*",
        "JRE*",
        "OSGi*",
    )

    manifest {
        attributes(mapOf("Main-Class" to "io.spine.tools.mc.java.protoc.Plugin"))
    }
    // Assemble "Fat-JAR" artifact containing all the dependencies.
    from(configurations.runtimeClasspath.get().map {
        when {
            it.isDirectory -> it
            else -> zipTree(it)
        }
    })
    // We should provide a classifier or else Protobuf Gradle plugin will substitute it with
    // an OS-specific one.
    archiveClassifier.set("exe")
    isZip64 = true  /* The archive has way too many items. So using the Zip64 mode. */

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
