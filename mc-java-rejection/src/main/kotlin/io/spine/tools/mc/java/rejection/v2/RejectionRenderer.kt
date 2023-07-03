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

package io.spine.tools.mc.java.rejection.v2

import com.google.protobuf.BoolValue
import com.google.protobuf.StringValue
import com.squareup.javapoet.JavaFile
import io.spine.code.proto.FileName
import io.spine.logging.WithLogging
import io.spine.protobuf.unpack
import io.spine.protodata.MessageType
import io.spine.protodata.ProtobufDependency
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.codegen.java.JavaRenderer
import io.spine.protodata.renderer.SourceFileSet
import io.spine.string.ti
import io.spine.tools.code.Indent
import java.nio.file.Path

public class RejectionRenderer: JavaRenderer(), WithLogging {

    private val context: CodegenContext by lazy {
        val typeSystem = bakeTypeSystem()
        CodegenContext(typeSystem)
    }

    private lateinit var sources: SourceFileSet

    override fun render(sources: SourceFileSet) {
        this.sources = sources
        val rejectionFiles = findRejectionFiles()
        rejectionFiles.forEach {
            generateRejection(it, context)
        }
    }

    private fun bakeTypeSystem(): TypeSystem {
        val types = TypeSystem.newBuilder()
        addSourceFiles(types)
        addDependencies(types)
        return types.build()
    }

    private fun addDependencies(types: TypeSystem.Builder) {
        val dependencies = select(ProtobufDependency::class.java).all()
        for (d in dependencies) {
            types.addFrom(d.file)
        }
    }

    private fun addSourceFiles(types: TypeSystem.Builder) {
        val files = select(ProtobufSourceFile::class.java).all()
        for (file in files) {
            types.addFrom(file)
        }
    }

    private fun findRejectionFiles(): List<ProtobufSourceFile> {
        val result = select(ProtobufSourceFile::class.java)
            .all()
            .filter { it.isRejections() }
        result.forEach { it.checkConventions() }
        return result
    }

    private fun generateRejection(proto: ProtobufSourceFile, context: CodegenContext) {
        if (proto.typeMap.isEmpty()) {
            logger.atWarning().log {
                "No rejection types found in the file `${proto.filePath}`."
            }
            return
        }
        logger.atDebug().log {
            """${System.lineSeparator()}
            Generating rejection classes for `${proto.filePath.value}`.
            Java package: `${proto.javaPackage()}`.
            Outer class name: `${proto.outerClassName()}`.
            Output directory: `${sources.outputRoot}`.            
            """.ti()
        }
        generateRejectionsFor(proto, context)
    }

    private fun generateRejectionsFor(proto: ProtobufSourceFile, context: CodegenContext) {
        proto.typeMap.values
            .filter { it.isTopLevel() }
            .forEach {
                val packageName = proto.javaPackage()
                val spec = KRThrowableSpec(packageName, it, context.typeSystem)
                val packageDir = sources.outputRoot.resolve(packageName.replace('.', '/'))
                val fileName = it.name.simpleName
                val file = packageDir.resolve("$fileName.java")

                println("**** Generating ${it.name.simpleName} to $file")

                spec.writeToFile(file)
            }
    }

    private fun KRThrowableSpec.writeToFile(file: Path) {
        val typeSpec = toPoet()
        val indent = Indent.of4()
        val javaFile = JavaFile.builder(packageName, typeSpec)
            .skipJavaLangImports(true)
            .indent(indent.toString())
            .build()
        val appendable = StringBuilder()
        javaFile.writeTo(appendable)
        sources.createFile(file, appendable.toString())
    }
}

private fun ProtobufSourceFile.isRejections(): Boolean {
    return filePath.value.endsWith("rejections.proto")
}

private fun MessageType.isTopLevel(): Boolean {
    return !hasDeclaredIn()
}

internal typealias RejectionFile = ProtobufSourceFile

/**
 * Ensures that this rejections file is configured according to the conventions.
 *
 * `java_multiple_files` option must be set to `false` or not specified, and
 * `java_outer_classname` must end with `Rejections` or absent.
 */
private fun RejectionFile.checkConventions() {
    checkNotMultipleFiles()
    checkOuterClassName()
}

private fun RejectionFile.checkNotMultipleFiles() {
    val optionName = "java_multiple_files"
    val javaMultipleFiles = file.optionList.find { it.name == optionName }
    javaMultipleFiles?.let {
        val explicitlySet = it.value.unpack<BoolValue>()
        check(!explicitlySet.value) {
            "A rejection file (`${filePath.value}`) should generate" +
                    " Java classes into a single source code file." +
                    " Please set `$optionName` option to `false`."
        }
    }
}

private const val OUTER_CLASS_NAME: String = "java_outer_classname"
private const val REJECTIONS_CLASS_SUFFIX: String = "Rejections"

internal fun RejectionFile.outerClassName(): String {
    val outerClassName = file.optionList.find { it.name == OUTER_CLASS_NAME }
    outerClassName?.let {
        val explicitlySet = it.value.unpack<StringValue>().value
        return explicitlySet
    }
    val nameOnly = file.path.value.substringAfterLast('/')
    val fn = FileName.of(nameOnly)
    return fn.toCamelCase()
}

private fun RejectionFile.checkOuterClassName() {
    val outerClassName = outerClassName()
    check(outerClassName.endsWith(REJECTIONS_CLASS_SUFFIX)) {
        "A rejection file (`${filePath.value}`) should have" +
                " the outer class named ending with `$REJECTIONS_CLASS_SUFFIX` or" +
                " do not have the option `$OUTER_CLASS_NAME` at all." +
                " Encountered outer class name: `$outerClassName`." +
                " Please rename the outer class or remove the option."
    }
}

@JvmName("javaPackageOf")
internal fun RejectionFile.javaPackage(): String {
    val optionName = "java_package"
    val javaPackage = file.optionList.find { it.name == optionName }
    return javaPackage?.value?.unpack<StringValue>()?.value ?: file.packageName
}
