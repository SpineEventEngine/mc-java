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
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.spine.server.route.Route

internal class RouteProcessor(
    private val codeGenerator: CodeGenerator,
    @Suppress("unused") internal val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allAnnotated = resolver.getSymbolsWithAnnotation(Route::class.qualifiedName!!)
        val routingFunctions = filterAndGroup(allAnnotated)
        routingFunctions.run {
            checkUsage(logger)
            generateCode()
        }
        val unprocessed = allAnnotated.filterNot { it.validate() }.toList()
        return unprocessed
    }

    private fun RoutingFunctions.generateCode() {
        forEach { (declaringClass, functions) ->
            val visitor = RouteVisitor(functions)
            declaringClass.accept(visitor, Unit)
            visitor.writeFile()
        }
    }

    private inner class RouteVisitor(
        private val functions: List<KSFunctionDeclaration>
    ) : KSVisitorVoid() {

        private lateinit var packageName: String
        private lateinit var originalFile: KSFile
        private lateinit var routingClass: TypeSpec.Builder

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            originalFile = classDeclaration.containingFile!!
            packageName = originalFile.packageName.asString()
            val className = classDeclaration.simpleName.asString() + "\$\$Routing"
            routingClass = TypeSpec.classBuilder(className)
            functions.forEach { it.accept(this, Unit) }
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            //TODO:2025-01-14:alexander.yevsyukov: Implement
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
}

/**
 * Maps a class to the list of routing functions it declares.  
 */
internal typealias RoutingFunctions = Map<KSClassDeclaration, List<KSFunctionDeclaration>>

/**
 * Filters all found annotated symbols to be valid instances of [KSFunctionDeclaration] and
 * groups them by declaring classes.
 */
private fun filterAndGroup(allAnnotated: Sequence<KSAnnotated>): RoutingFunctions {
    val declarations = allAnnotated
        .filter { it is KSFunctionDeclaration && it.validate() }
        .map { it as KSFunctionDeclaration }
    val routingFunctions = declarations.groupByClasses()
    return routingFunctions
}

/**
 * Groups function declarations by declaring classes.
 */
private fun Sequence<KSFunctionDeclaration>.groupByClasses(): RoutingFunctions =
    filter { it.parentDeclaration!! is KSClassDeclaration }
        .groupBy { it.parentDeclaration!! as KSClassDeclaration }

/**
 * Validates routing function declarations.
 *
 * @see SignatureCheck
 */
private fun RoutingFunctions.checkUsage(logger: KSPLogger) {
    forEach { (declaringClass, functions) ->
        SignatureCheck(declaringClass, functions, logger).apply()
    }
}
