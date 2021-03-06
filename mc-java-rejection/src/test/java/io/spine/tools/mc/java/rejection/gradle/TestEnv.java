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

package io.spine.tools.mc.java.rejection.gradle;

import io.spine.code.java.PackageName;
import io.spine.code.proto.FieldName;
import io.spine.tools.java.fs.DefaultJavaPaths;
import io.spine.tools.java.fs.FileName;
import io.spine.tools.java.fs.JavaFiles;

import java.nio.file.Path;
import java.util.Arrays;

import static io.spine.tools.code.SourceSetName.main;
import static java.lang.String.format;

final class TestEnv {

    private static final String CLASS_COMMENT =
            "The rejection definition to test Javadoc generation.";
    private static final String REJECTION_NAME = "Rejection";
    private static final String FIRST_FIELD_COMMENT = "The rejection ID.";
    private static final FieldName FIRST_FIELD = FieldName.of("id");
    private static final String SECOND_FIELD_COMMENT = "The rejection message.";
    private static final FieldName SECOND_FIELD = FieldName.of("rejection_message");

    private static final PackageName JAVA_PACKAGE = PackageName.of("io.spine.sample.rejections");
    private static final FileName REJECTION_FILE_NAME = FileName.forType(REJECTION_NAME);

    /** Prevents instantiation of this utility class. */
    private TestEnv() {
    }

    static Path rejectionsJavadocThrowableSource(Path projectDir) {
        var javaPackage = JavaFiles.toDirectory(JAVA_PACKAGE);
        return DefaultJavaPaths.at(projectDir)
                               .generatedProto()
                               .spine(main)
                               .path()
                               .resolve(javaPackage)
                               .resolve(REJECTION_FILE_NAME.value());
    }

    static Iterable<String> rejectionWithJavadoc() {
        return Arrays.asList(
                "syntax = \"proto3\";",
                "package spine.sample.rejections;",
                "option java_package = \"" + JAVA_PACKAGE + "\";",
                "option java_multiple_files = false;",

                "// " + CLASS_COMMENT,
                "message " + REJECTION_NAME + " {",

                "    // " + FIRST_FIELD_COMMENT,
                "    int32 " + FIRST_FIELD + " = 1; // Is not a part of Javadoc.",

                "    // " + SECOND_FIELD_COMMENT,
                "    string " + SECOND_FIELD + " = 2;",

                "    bool hasNoComment = 3;",
                "}"
        );
    }

    static String expectedClassComment() {
        return wrappedInPreTag(CLASS_COMMENT)
                + " Rejection based on proto type  " +
                "{@code " + JAVA_PACKAGE + '.' + REJECTION_NAME+ '}';
    }

    static String expectedBuilderClassComment() {
        return format("The builder for the  {@code %s}  rejection.", REJECTION_NAME);
    }

    static String expectedFirstFieldComment() {
        return wrappedInPreTag(FIRST_FIELD_COMMENT);

    }

    static String expectedSecondFieldComment() {
        return wrappedInPreTag(SECOND_FIELD_COMMENT);
    }

    private static String wrappedInPreTag(String commentText) {
        return "<pre> " + commentText + " </pre>";
    }
}
