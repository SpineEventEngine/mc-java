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

package io.spine.tools.mc.java.annotation

import io.spine.protodata.ServiceName
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.GrpcServiceConvention
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.tools.mc.annotation.ServiceAnnotations

internal class ServiceAnnotationRenderer :
    AnnotationRenderer<ServiceAnnotations>(ServiceAnnotations::class.java) {

    private val convention by lazy {
        GrpcServiceConvention(typeSystem!!)
    }

    override fun annotateType(state: ServiceAnnotations, annotationClass: Class<out Annotation>) {
        val serviceClass = convention.declarationFor(state.service).name
        val annotation = ApiTypeAnnotation(serviceClass, annotationClass)
        annotation.registerWith(context!!)
        annotation.renderSources(sources)
    }

    override fun suitableFor(sources: SourceFileSet): Boolean =
        sources.outputRoot.endsWith("grpc")
}

private class ServiceAnnotation<T : Annotation>(
    private val serviceName: ServiceName,
    className: ClassName,
    annotationClass: Class<T>
) : ApiTypeAnnotation<T>(className, annotationClass) {

    private val convention by lazy {
        GrpcServiceConvention(typeSystem!!)
    }

    override fun shouldAnnotate(file: SourceFile): Boolean {
        val declaration = convention.declarationFor(serviceName)
        val fileMatches = declaration.path.endsWith(file.relativePath)
        return fileMatches && super.shouldAnnotate(file)
    }
}
