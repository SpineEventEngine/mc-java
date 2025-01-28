/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.routing

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.spine.server.route.CommandRoutingSetup
import io.spine.server.route.EventRoutingSetup
import io.spine.server.route.StateRoutingSetup

internal sealed class RouteVisitor<F : RouteFun>(
    private val functions: List<F>,
    protected val codeGenerator: CodeGenerator,
    protected val environment: Environment,
) : KSVisitorVoid() {

    private lateinit var packageName: String
    private lateinit var originalFile: KSFile
    private lateinit var routingClass: TypeSpec.Builder

    protected abstract val classNameSuffix: String

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        originalFile = classDeclaration.containingFile!!
        packageName = originalFile.packageName.asString()
        val className = classDeclaration.simpleName.asString() + classNameSuffix
        routingClass = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.INTERNAL)
        functions.forEach { it.fn.accept(this, Unit) }
    }

    protected abstract fun generateCode(function: KSFunctionDeclaration)

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        generateCode(function)
    }

    fun writeFile() {
        val cls = routingClass.build()
        val code = FileSpec.builder(packageName, cls.name!!)
            .addType(cls)
            .build()
        val deps = Dependencies(true, originalFile)
        code.writeTo(codeGenerator, deps)
    }
}

internal class CommandRouteVisitor(
    functions: List<CommandRouteFun>,
    codeGenerator: CodeGenerator,
    environment: Environment
) : RouteVisitor<CommandRouteFun>(functions, codeGenerator, environment)  {

    override val classNameSuffix: String = CommandRoutingSetup.classSuffix

    override fun generateCode(function: KSFunctionDeclaration) {
        //TODO:2025-01-22:alexander.yevsyukov: Implement.
    }
}

internal class EventRouteVisitor(
    functions: List<EventRouteFun>,
    codeGenerator: CodeGenerator,
    environment: Environment
) : RouteVisitor<EventRouteFun>(functions, codeGenerator, environment) {

    override val classNameSuffix: String = EventRoutingSetup.classSuffix

    override fun generateCode(function: KSFunctionDeclaration) {
        //TODO:2025-01-22:alexander.yevsyukov: Implement.
    }
}

internal class StateUpdateRouteVisitor(
    functions: List<StateUpdateRouteFun>,
    codeGenerator: CodeGenerator,
    environment: Environment
) : RouteVisitor<StateUpdateRouteFun>(functions, codeGenerator, environment) {

    override val classNameSuffix: String = StateRoutingSetup.classSuffix

    override fun generateCode(function: KSFunctionDeclaration) {
        //TODO:2025-01-22:alexander.yevsyukov: Implement.
    }
}
