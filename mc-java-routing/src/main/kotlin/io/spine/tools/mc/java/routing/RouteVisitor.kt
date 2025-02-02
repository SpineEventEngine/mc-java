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
import io.spine.string.Indent
import io.spine.tools.mc.java.GeneratedAnnotation
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
        handleRouteFunctions()
    }

    @OverridingMethodsMustInvokeSuper
    protected open fun createClass(className: String) {
        val generated = GeneratedAnnotation.forKotlinPoet()
        val autoService = AnnotationSpec.builder(AutoService::class)
            .addMember("%T::class", setupType.setupClass)
            .build()

        routingClass = TypeSpec.classBuilder(className)
            .addKdoc(classKDoc())
            // Must be `public` because created reflectively via `AutoService`
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(generated)
            .addAnnotation(autoService)

        val superInterface = setupType.type
            .replace(listOf(idClassTypeArgument))
            .toTypeName()
        routingClass.addSuperinterface(superInterface)
        addEntityClassFunction()
    }

    private fun classKDoc(): CodeBlock = CodeBlock.builder()
        .add("Configures [%T] of the repository managing [%T] instances.\n\n",
            setupType.routingClass.asClassName(),
            entityClass.type.toClassName())
        .add("@see %T.apply()\n", setupType.setupClass.asClassName())
        .build()

    /**
     * Adds the method that overrides [io.spine.server.route.RoutingSetup.entityClass].
     */
    private fun addEntityClassFunction() {
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

    private fun handleRouteFunctions() {
        openSetupFunction()
        functions.forEach { addRoute(it) }
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
            .beginControlFlow("%N.run", paramName)
    }

    protected abstract fun addRoute(fn: F)

    private fun closeSetupFunction() {
        routingRunBlock.endControlFlow()
        setupFun.addCode(routingRunBlock.build())
        routingClass.addFunction(setupFun.build())
    }

    fun writeFile() {
        val cls = routingClass.build()
        val code = FileSpec.builder(packageName, cls.name!!)
            .indent(Indent.defaultJavaIndent.value)
            .addType(cls)
            .build()
        val deps = Dependencies(true, originalFile)
        code.writeTo(environment.codeGenerator, deps)
    }

    companion object {

        internal fun process(
            allValid: Sequence<KSFunctionDeclaration>,
            environment: Environment
        ) {
            val qualified = RouteSignature.qualify(allValid, environment)
            CommandRouteVisitor.process(qualified, environment)
            EventRouteVisitor.process(qualified, environment)
            StateUpdateRouteVisitor.process(qualified, environment)
        }

        internal inline fun <V : RouteVisitor<F>, reified F : RouteFun> runVisitors(
            qualified: List<RouteFun>,
            createVisitor: (List<F>) -> V
        ) {
            val routing = qualified.filterIsInstance<F>()
            val grouped = routing.groupByClasses()
            grouped.forEach { (declaringClass, functions) ->
                val v = createVisitor(functions)
                declaringClass.accept(v, Unit)
                v.writeFile()
            }
        }
    }
}

/**
 * Groups this list of route functions by the classes in which they are declared.
 *
 * The grouped functions are then sorted by the [first parameter type][RouteFun.messageParameter],
 * putting classes first, and then less abstract interfaces, and so on down to more abstract ones.
 *
 * This order will be used when adding routes for each [entity class][RouteFun.declaringClass].
 *
 * @see RouteFunComparator
 */
private fun <F : RouteFun> List<F>.groupByClasses(): Map<EntityClass, List<F>> =
    groupBy { it.declaringClass }
        .mapValues { (_, list) ->
            RouteFunComparator.sort(list)
        }

/**
 * Compares two [RouteFun] instances by their [messageParameter][RouteFun.messageParameter]
 * properties, putting more abstract type further in the sorting order.
 */
private class RouteFunComparator : Comparator<RouteFun> {

    @Suppress("ReturnCount")
    override fun compare(o1: RouteFun, o2: RouteFun): Int {
        val m1 = o1.messageParameter
        val m2 = o2.messageParameter

        if (m1 == m2) {
            return 0
        }
        // An interface should come after a class in the sorting.
        if (m1.isInterface && !m2.isInterface) {
            return 1
        }
        if (!m1.isInterface && m2.isInterface) {
            return -1
        }
        // Both are either classes or interfaces.
        // The one that is more abstract goes further in sorting.
        if (m1.isAssignableFrom(m2)) {
            return 1
        }
        if (m2.isAssignableFrom(m1)) {
            return -1
        }
        val n1 = m1.declaration.qualifiedName?.asString()
        val n2 = m2.declaration.qualifiedName?.asString()
        return compareValues(n1, n2)
    }

    companion object {

        fun <F : RouteFun> sort(list: List<F>): List<F> {
            val comparator = RouteFunComparator()
            return list.sortedWith(comparator)
        }
    }
}
