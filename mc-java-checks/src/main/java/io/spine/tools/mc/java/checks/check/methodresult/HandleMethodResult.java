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

package io.spine.tools.mc.java.checks.check.methodresult;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.AbstractReturnValueIgnored;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.CheckReturnValue;
import com.google.errorprone.bugpatterns.checkreturnvalue.PackagesRule;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicy;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicyEvaluator;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicyEvaluator.MethodInfo;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.google.protobuf.Message;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.google.errorprone.BugPattern.LinkType.NONE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.checkreturnvalue.AutoValueRules.autoBuilders;
import static com.google.errorprone.bugpatterns.checkreturnvalue.AutoValueRules.autoValueBuilders;
import static com.google.errorprone.bugpatterns.checkreturnvalue.AutoValueRules.autoValues;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ExternalCanIgnoreReturnValue.externalIgnoreList;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ProtoRules.mutableProtos;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ProtoRules.protoBuilders;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicy.EXPECTED;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicy.OPTIONAL;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicy.UNSPECIFIED;
import static com.google.errorprone.bugpatterns.checkreturnvalue.Rules.globalDefault;
import static com.google.errorprone.bugpatterns.checkreturnvalue.Rules.mapAnnotationSimpleName;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.util.ASTHelpers.enclosingElements;

/**
 * An ErrorProne check which ensures that the values returned from methods are not
 * accidentally ignored.
 *
 * <p>This check is a substitute for {@link CheckReturnValue}.
 * The difference is that this check ignores invocations of mutating methods on message builders.
 *
 * <p>The check may be suppressed in the same ways as {@link CheckReturnValue}.
 */
@AutoService(BugChecker.class)
@BugPattern(
        altNames = {"CheckReturnValue", "ResultOfMethodCallIgnored", "ReturnValueIgnored"},
        summary = HandleMethodResult.SUMMARY,
        severity = ERROR,
        linkType = NONE
)
public final class HandleMethodResult extends AbstractReturnValueIgnored {

    private static final long serialVersionUID = 0L;

    static final String SUMMARY =
            "Ignored return value of method that is annotated with `@CheckReturnValue`";
    private static final Pattern ACCESSOR_PREFIX = Pattern.compile("(set|add|put|merge|remove).+");

    private static final String CHECK_RETURN_VALUE = "CheckReturnValue";
    private static final String CAN_IGNORE_RETURN_VALUE = "CanIgnoreReturnValue";

    public static final String CHECK_ALL_CONSTRUCTORS = "CheckReturnValue:CheckAllConstructors";
    public static final String CHECK_ALL_METHODS = "CheckReturnValue:CheckAllMethods";

    static final String CRV_PACKAGES = "CheckReturnValue:Packages";

    private static final MethodInfo<VisitorState, Symbol, MethodSymbol> METHOD_INFO =
            new MethodInfo<>() {
                @Override
                public Stream<Symbol> scopeMembers(
                        ResultUseRule.RuleScope scope, MethodSymbol method, VisitorState context) {
                    switch (scope) {
                        case ENCLOSING_ELEMENTS:
                            return enclosingElements(method)
                                    .filter(s -> s instanceof ClassSymbol
                                                 || s instanceof PackageSymbol);
                        case GLOBAL:
                        case METHOD:
                            return Stream.of(method);
                        default:
                            break;
                    }
                    throw new AssertionError(scope);
                }

                @Override
                public MethodKind getMethodKind(MethodSymbol method) {
                    switch (method.getKind()) {
                        case METHOD:
                            return MethodKind.METHOD;
                        case CONSTRUCTOR:
                            return MethodKind.CONSTRUCTOR;
                        default:
                            return MethodKind.OTHER;
                    }
                }
            };

    @SuppressWarnings("NonSerializableFieldInSerializableClass")
    private final ResultUsePolicyEvaluator<VisitorState, Symbol, Symbol.MethodSymbol> evaluator;

    public HandleMethodResult() {
        super(ConstantExpressions.fromFlags(ErrorProneFlags.empty()));

        var builder = ResultUsePolicyEvaluator.builder(METHOD_INFO).addRules(
                // The order of these rules matters somewhat because when checking a method, we'll
                // evaluate them in the order they're listed here and stop as soon as one of them
                // returns a result. The order shouldn't matter because most of these, except
                // perhaps the external ignore list, are equivalent in importance, and
                // we should be checking declarations to ensure they aren't producing differing
                // results (i.e. ensuring an @AutoValue.Builder setter method isn't annotated @CRV).
                mapAnnotationSimpleName(CHECK_RETURN_VALUE, EXPECTED),
                mapAnnotationSimpleName(CAN_IGNORE_RETURN_VALUE, OPTIONAL),
                protoBuilders(),
                mutableProtos(),
                autoValues(),
                autoValueBuilders(),
                autoBuilders(),

                // This is conceptually lower precedence than the above rules.
                externalIgnoreList()
        );

        var flags = ErrorProneFlags.empty();
        flags.getList(CRV_PACKAGES)
             .ifPresent(packagePatterns -> builder.addRule(
                     PackagesRule.fromPatterns(packagePatterns)));
        this.evaluator = builder
                .addRule(
                        globalDefault(
                                defaultPolicy(flags, CHECK_ALL_METHODS),
                                defaultPolicy(flags, CHECK_ALL_CONSTRUCTORS))
                )
                .build();
    }

    @Override
    public Matcher<ExpressionTree> specializedMatcher() {
        Matcher<ExpressionTree> checkReturnValue =
                (tree, state) -> getMethodPolicy(tree, state) == EXPECTED;

        var notBuilderSetter = not(builderSetter());
        return allOf(checkReturnValue, notBuilderSetter);
    }

    private static Matcher<ExpressionTree> builderSetter() {
        return MethodMatchers
                .instanceMethod()
                .onDescendantOf(Message.Builder.class.getName())
                .withNameMatching(ACCESSOR_PREFIX);
    }

    @Override
    public ResultUsePolicy getMethodPolicy(ExpressionTree expression, VisitorState state) {
        return methodSymbol(expression)
                .map(method -> evaluator.evaluate(method, state))
                .orElse(UNSPECIFIED);
    }

    private static Optional<Symbol.MethodSymbol> methodSymbol(ExpressionTree tree) {
        var sym = ASTHelpers.getSymbol(tree);
        return sym instanceof Symbol.MethodSymbol
               ? Optional.of((Symbol.MethodSymbol) sym)
               : Optional.empty();
    }

    private static Optional<ResultUsePolicy> defaultPolicy(ErrorProneFlags flags, String flag) {
        return flags.getBoolean(flag).map(check -> check ? EXPECTED : OPTIONAL);
    }
}
