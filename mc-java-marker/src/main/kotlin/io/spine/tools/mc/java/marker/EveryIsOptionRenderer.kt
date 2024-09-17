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

package io.spine.tools.mc.java.marker

import com.google.protobuf.Message
import com.intellij.psi.PsiJavaFile
import io.spine.option.IsOption
import io.spine.protodata.MessageType
import io.spine.protodata.ProtoFileHeader
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.JavaRenderer
import io.spine.protodata.java.JavaTypeName.Companion.PACKAGE_SEPARATOR
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.java.javaPackage
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.tools.code.Java
import io.spine.tools.java.reference
import io.spine.tools.mc.java.CreateInterface
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.ImplementInterface
import io.spine.tools.mc.java.SuperInterface
import io.spine.tools.mc.java.superInterface
import io.spine.tools.psi.java.addFirst
import io.spine.tools.psi.java.execute
import io.spine.tools.psi.java.topLevelClass
import org.checkerframework.checker.signature.qual.FullyQualifiedName

internal class EveryIsOptionRenderer : JavaRenderer() {

    /**
     * The super base interface for all generated interfaces.
     */
    private val superBase = superInterface {
        name = Message::class.java.reference
    }

    private lateinit var sources: SourceFileSet

    override fun render(sources: SourceFileSet) {
        if (!sources.hasJavaRoot) {
            return
        }
        this.sources = sources
        val views = findViews()
        views.forEach {
            execute {
                it.doRender()
            }
        }
    }

    private fun findViews(): Set<EveryIsMessages> {
        val found = select(EveryIsMessages::class.java).all()
        return found
    }

    private fun EveryIsMessages.doRender() {
        val interfaceName = option.qualifiedJavaType(header)
        createInterface(interfaceName)
        implementInTypes(interfaceName)
    }

    private fun EveryIsMessages.createInterface(interfaceName: @FullyQualifiedName String) {
        if (option.generate) {
            val iface = ClassName.guess(interfaceName)
            val createdFile = CreateInterface(iface, superBase).render(sources)
            annotate(createdFile)
        }
    }

    private fun annotate(file: SourceFile<Java>) {
        val psiFile = file.psi() as PsiJavaFile
        val annotation = GeneratedAnnotation.create()
        psiFile.topLevelClass.addFirst(annotation)
        val updatedCode = psiFile.text
        file.overwrite(updatedCode)
    }

    private fun EveryIsMessages.implementInTypes(interfaceName: @FullyQualifiedName String) {
        val interfaceProto = superInterface {
            name = interfaceName
        }
        typeList.forEach {
            it.implementInterface(interfaceProto)
        }
    }

    private fun MessageType.implementInterface(superInterface: SuperInterface) {
        val file = sources.javaFileOf(this)
        val action = ImplementInterface(this, file, superInterface, context!!)
        action.render()
    }
}

public fun IsOption.qualifiedJavaType(header: ProtoFileHeader): @FullyQualifiedName String {
    check(javaType.isNotEmpty() && javaType.isNotBlank()) {
        "The value of `java_type` must not be empty or blank. Got: `\"$javaType\"`."
    }
    return if (javaType.isQualified) {
        javaType
    } else {
        "${header.javaPackage()}.$javaType"
    }
}

private val String.isQualified: Boolean
    get() = contains(PACKAGE_SEPARATOR)

