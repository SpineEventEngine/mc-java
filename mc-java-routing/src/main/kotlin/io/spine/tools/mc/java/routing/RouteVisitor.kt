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

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.spine.server.entity.Entity
import io.spine.tools.mc.java.routing.Environment.SetupType

internal sealed class RouteVisitor<F : RouteFun>(
    protected val setupType: SetupType,
    private val functions: List<F>,
    protected val environment: Environment,
) : KSVisitorVoid() {

    private lateinit var packageName: String
    private lateinit var originalFile: KSFile

    protected abstract val classNameSuffix: String

    protected lateinit var routingClass: TypeSpec.Builder
    protected lateinit var setupFun: FunSpec.Builder
    protected lateinit var routingRunBlock: CodeBlock.Builder

    val entityClass: EntityClass by lazy {
        val fn = functions.first()
        fn.declaringClass
    }

    private val idClassTypeArgument: KSTypeArgument by lazy {
        entityClass.idClassTypeArgument
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        originalFile = classDeclaration.containingFile!!
        packageName = originalFile.packageName.asString()
        val className = classDeclaration.simpleName.asString() + classNameSuffix
        createClass(className)
        handleFunctions()
    }

    @OverridingMethodsMustInvokeSuper
    protected open fun createClass(className: String) {
        val annotation = AnnotationSpec.builder(AutoService::class)
            .addMember("%T::class", setupType.setupClass)
            .build()

        routingClass = TypeSpec.classBuilder(className)
            // Must be `public` because created reflectively via `AutoService`
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(annotation)

        val superInterface = setupType.type
            .replace(listOf(idClassTypeArgument))
            .toTypeName()
        routingClass.addSuperinterface(superInterface)
        addEntityClassFunction()
    }

    /**
     * Adds the method that overrides [io.spine.server.route.RoutingSetup.entityClass].
     */
    protected fun addEntityClassFunction() {
        val entityType = Entity::class.asClassName().parameterizedBy(
            idClassTypeArgument.type!!.toTypeName(),
            STAR
        )
        val classOfEntityInterface = Class::class.asClassName().parameterizedBy(
            WildcardTypeName.producerOf(entityType)
        )

        val funSpec = FunSpec.builder("entityClass")
            .addModifiers(KModifier.OVERRIDE)
            .returns(classOfEntityInterface)
            .addCode("return %T::class.java\n", entityClass.type.toClassName())
            .build()

        routingClass.addFunction(funSpec)
    }

    private fun handleFunctions() {
        openSetupFunction()
        functions.forEach { it.fn.accept(this, Unit) }
        closeSetupFunction()
    }

    private fun openSetupFunction() {
        val paramName = "routing"
        val paramType = setupType.routingClass.asClassName()
            .parameterizedBy(idClassTypeArgument.type!!.toTypeName())
        val param = ParameterSpec.builder(paramName, paramType)
        setupFun = FunSpec.builder("setup")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(param.build())

        routingRunBlock = CodeBlock.builder()
            .add("%N.run {\n", paramName)
    }

    private fun closeSetupFunction() {
        routingRunBlock.add("}\n")
        setupFun.addCode(routingRunBlock.build())
        routingClass.addFunction(setupFun.build())
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        addRoute(function)
    }

    protected abstract fun addRoute(function: KSFunctionDeclaration)

    fun writeFile() {
        val cls = routingClass.build()
        val code = FileSpec.builder(packageName, cls.name!!)
            .addType(cls)
            .build()
        val deps = Dependencies(true, originalFile)
        code.writeTo(environment.codeGenerator, deps)
    }
}

internal class CommandRouteVisitor(
    functions: List<CommandRouteFun>,
    environment: Environment
) : RouteVisitor<CommandRouteFun>(
    environment.commandRoutingSetup,
    functions,
    environment
) {
    override val classNameSuffix: String = "CommandRouting"

    override fun addRoute(function: KSFunctionDeclaration) {
        //TODO:2025-01-22:alexander.yevsyukov: Implement.
    }
}

internal class EventRouteVisitor(
    functions: List<EventRouteFun>,
    environment: Environment
) : RouteVisitor<EventRouteFun>(
    environment.eventRoutingSetup,
    functions,
    environment
) {
    override val classNameSuffix: String = "EventRouting"

    override fun createClass(className: String): Unit = environment.run {
        super.createClass(className)
    }

    override fun addRoute(function: KSFunctionDeclaration) {
        //TODO:2025-01-22:alexander.yevsyukov: Implement.
    }
}

internal class StateUpdateRouteVisitor(
    functions: List<StateUpdateRouteFun>,
    environment: Environment
) : RouteVisitor<StateUpdateRouteFun>(
    environment.stateRoutingSetup,
    functions,
    environment
) {

    override val classNameSuffix: String = "StateUpdateRouting"

    override fun addRoute(function: KSFunctionDeclaration) {
        //TODO:2025-01-22:alexander.yevsyukov: Implement.
    }
}
