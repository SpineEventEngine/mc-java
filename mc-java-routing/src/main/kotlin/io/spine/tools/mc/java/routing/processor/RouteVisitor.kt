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

package io.spine.tools.mc.java.routing.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSVisitorVoid
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
import io.spine.tools.mc.java.routing.processor.Environment.SetupType

/**
 * The base class for code generators implementing routing setup classes.
 *
 * The type of the generated setup class is determined by the [setup] property.
 * The type of the generated route functions is specified by the generic parameter [F].
 *
 * The visitor generates a class named after the class declaring routing function(s)
 * using the pattern: [entityClass] + [classNameSuffix]. The class is generated
 * in the same package with the [entityClass].
 *
 * The generated class will be annotated with the [AutoService] annotation which
 * would accept the interface implemented by the generated class as the argument for
 * the annotation. E.g., [EventRoutingSetup][io.spine.server.route.setup.EventRoutingSetup] or
 * [CommandRoutingSetup][io.spine.server.route.setup.CommandRoutingSetup].
 * The reference to this interface is passed as the value of
 *  the [cls][SetupType.cls] property of the [setup] parameter.
 *
 * @param F The type of route functions handled by the visitor.
 * @property setup The type of the routing setup class generated by this visitor.
 * @property functions The route functions declared in the [entityClass].
 * @property environment The environment for code generation.
 */
internal sealed class RouteVisitor<F : RouteFun>(
    private val setup: SetupType,
    private val functions: List<F>,
    protected val environment: Environment
) : KSVisitorVoid() {

    /**
     * The suffix added to the generated class.
     */
    protected abstract val classNameSuffix: String

    /**
     * The name of the package for the generated class.
     */
    private lateinit var packageName: String

    /**
     * The file declaring the class with the routing functions.
     */
    private lateinit var originalFile: KSFile

    /**
     * The builder of the generated class.
     */
    protected lateinit var routingClass: TypeSpec.Builder

    /**
     * The builder for the generated
     * [setup][io.spine.server.route.setup.RoutingSetup.setup] function.
     */
    protected lateinit var setupFun: FunSpec.Builder

    /**
     * The builder for the `run` block in the [setup][setupFun] function.
     *
     * The generated block will look like this:
     * ```kotlin
     * routing.run {
     *    // Route functions are added here. This comment will not be added.
     * }
     * ```
     */
    protected lateinit var routingRunBlock: CodeBlock.Builder

    /**
     * The class declaring route functions.
     */
    protected val entityClass: EntityClass by lazy {
        val fn = functions.first()
        fn.declaringClass
    }

    /**
     * The type of the entity identifiers.
     */
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

    private fun createClass(className: String) {
        val generated = GeneratedAnnotation.forKotlinPoet()
        val autoService = AnnotationSpec.builder(AutoService::class)
            .addMember("%T::class", setup.cls)
            .build()

        routingClass = TypeSpec.classBuilder(className)
            .addKdoc(classKDoc())
            // Must be `public` because created reflectively via `AutoService`
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(generated)
            .addAnnotation(autoService)

        val superInterface = setup.type
            .replace(listOf(idClassTypeArgument))
            .toTypeName()
        routingClass.addSuperinterface(superInterface)
        addEntityClassFunction()
    }

    private fun classKDoc(): CodeBlock = CodeBlock.builder()
        .add("Configures [%T] of the repository managing [%T] instances.\n\n",
            setup.routingClass.asClassName(),
            entityClass.type.toClassName())
        .add("@see %T.apply()\n", setup.cls.asClassName())
        .build()

    /**
     * Adds the method that overrides [io.spine.server.route.setup.RoutingSetup.entityClass].
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
        val paramType = setup.routingClass.asClassName()
            .parameterizedBy(idClassTypeArgument.type!!.toTypeName())
        val param = ParameterSpec.builder(paramName, paramType)
        setupFun = FunSpec.builder("setup")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(param.build())

        routingRunBlock = CodeBlock.builder()
            .beginControlFlow("%N.run", paramName)
    }

    /**
     * The callback to add a route function to the routing schema.
     */
    protected abstract fun addRoute(fn: F)

    private fun closeSetupFunction() {
        routingRunBlock.endControlFlow()
        setupFun.addCode(routingRunBlock.build())
        routingClass.addFunction(setupFun.build())
    }

    private fun writeFile() {
        val cls = routingClass.build()
        val code = FileSpec.builder(packageName, cls.name!!)
            .indent(Indent.defaultJavaIndent.value)
            .addType(cls)
            .build()
        val deps = Dependencies(true, originalFile)
        code.writeTo(environment.codeGenerator, deps)
    }

    companion object {

        /**
         * The name of the inline extension functions for classes extending
         * [MessageRouting][io.spine.server.route.MessageRouting] which are used in
         * the generated code of [RoutingSetup][io.spine.server.route.setup.RoutingSetup] classes.
         */
        const val ROUTE_FUN_NAME: String = "route"

        /**
         * The name of the inline extension function for classing extending
         * [MessageRouting][io.spine.server.route.MessageRouting] which are used in
         * the generated code of [RoutingSetup][io.spine.server.route.setup.RoutingSetup] classes
         * for returning only [one entity identifier][io.spine.server.route.Unicast].
         */
        const val UNICAST_FUN_NAME: String = "unicast"

        /**
         * Runs all the visitors through the given sequence of found functions.
         *
         * @see CommandRouteVisitor
         * @see EventRouteVisitor
         * @see StateUpdateRouteVisitor
         */
        fun process(
            allValid: Sequence<KSFunctionDeclaration>,
            environment: Environment
        ) {
            val qualified = RouteSignature.qualify(allValid, environment)
            CommandRouteVisitor.process(qualified, environment)
            EventRouteVisitor.process(qualified, environment)
            StateUpdateRouteVisitor.process(qualified, environment)
        }

        /**
         * Runs a visitor through the list of given functions.
         *
         * The visitor runs only through a sub-list of functions containing only
         * instances of the generic parameter [F] of this function.
         * For example, a [CommandRouteVisitor] ([V]) will only run
         * through [CommandRouteFun] ([F]) instances.
         *
         * The function also performs the check for
         * [duplicated route functions][EntityClass.hasDuplicatedRoutes] per declaring class.
         * If such duplicates are found, errors will be logged and <em>all</em> the functions of
         * the declaring class will not be processed by the visitor.
         *
         * @param V The type of the [RouteVisitor] used during the traversal.
         * @param F The type of the [RouteFun] processed by the visitor.
         * @param allFunctions All the routing functions found by the annotation processor and
         *   [transformed][Qualifier] into [RouteFun] instances.
         * @param environment The environment of the code generation.
         * @param createVisitor The function to create an instance of the visitor class.
         */
        inline fun <V : RouteVisitor<F>, reified F : RouteFun> runVisitors(
            allFunctions: List<RouteFun>,
            environment: Environment,
            createVisitor: (List<F>) -> V
        ) {
            val routing = allFunctions.filterIsInstance<F>()
            val grouped = routing.groupByClasses()
            grouped.forEach { (declaringClass, functions) ->
                if (!declaringClass.hasDuplicatedRoutes(functions, environment)) {
                val v = createVisitor(functions)
                    declaringClass.accept(v, Unit)
                    v.writeFile()
                }
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
