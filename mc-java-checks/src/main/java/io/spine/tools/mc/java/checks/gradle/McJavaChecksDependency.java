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

package io.spine.tools.mc.java.checks.gradle;

import io.spine.logging.WithLogging;
import io.spine.tools.mc.java.checks.Artifacts;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.tools.gradle.Artifact.SPINE_TOOLS_GROUP;
import static io.spine.tools.mc.java.checks.Artifacts.mcJavaChecks;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Adds a {@code spine-mc-java-checks} dependency to the given project {@link Configuration}.
 */
public final class McJavaChecksDependency implements WithLogging {

    /** The configuration to be extended. */
    private final Configuration configuration;

    /** The dependency to be added. */
    private final Dependency dependency;

    private McJavaChecksDependency(Configuration cfg) {
        this.configuration = cfg;
        this.dependency = checksDependency();
    }

    private static DefaultExternalModuleDependency checksDependency() {
        var version = Artifacts.mcJavaChecksVersion();
        return new DefaultExternalModuleDependency(
                SPINE_TOOLS_GROUP, Artifacts.MC_JAVA_CHECKS_ARTIFACT, version
        );
    }

    /**
     * Adds the dependency of the Spine Model Checks to the given configuration.
     *
     * @param project
     *         the project to which apply the dependency
     * @return true if the configuration was applied
     */
    public static boolean addTo(Project project) {
        checkNotNull(project);
        var cfg = AnnotationProcessorConfiguration.findOrCreateIn(project);
        var dep = new McJavaChecksDependency(cfg);
        var result = dep.addDependency();
        return result;
    }

    /**
     * Adds the dependency to the project configuration.
     *
     * @return {@code true} if the operation was successful, {@code false} otherwise
     */
    private boolean addDependency() {
        var helper = new ResolutionHelper();
        if (helper.wasResolved()) {
            addDependencyTo(configuration);
            return true;
        } else {
            helper.logUnresolved();
            return false;
        }
    }

    /**
     * Adds the dependency to the project configuration.
     */
    private void addDependencyTo(Configuration cfg) {
        logger().atDebug().log(() -> format(
            "Adding a dependency on `%s` to the `%s` configuration.", mcJavaChecks(), cfg));
        var dependencies = cfg.getDependencies();
        dependencies.add(dependency);
    }

    /**
     * Assists with checking if the dependency can be resolved, and if not, helps with
     * logging error diagnostics.
     */
    private final class ResolutionHelper {

        private final ResolutionResult resolutionResult;
        private @Nullable UnresolvedDependencyResult unresolved;

        private ResolutionHelper() {
            var configCopy = configuration.copy();
            addDependencyTo(configCopy);
            resolutionResult =
                    configCopy.getIncoming()
                              .getResolutionResult();
        }

        /**
         * Verifies if the {@link #dependency} to be added was resolved, returning {@code true}
         * if so.
         *
         * <p>If the {@link #dependency} was not resolved, the corresponding
         * {@link UnresolvedDependencyResult} is {@linkplain #unresolved stored} for
         * future logging needs.
         */
        private boolean wasResolved() {
            try {
                var allDeps = resolutionResult.getAllDependencies();
                var group = requireNonNull(dependency.getGroup());
                var name = dependency.getName();
                for (var dep : allDeps) {
                    if (dep instanceof UnresolvedDependencyResult) {
                        var unresolved = (UnresolvedDependencyResult) dep;
                        var attempted = unresolved.getAttempted();
                        var displayName = attempted.getDisplayName();
                        if (displayName.contains(group) && displayName.contains(name)) {
                            this.unresolved = unresolved;
                            return false;
                        }
                    }
                }
            } catch (RuntimeException e) {
                // Something went wrong during the resolution.
                return false;
            }
            return true;
        }

        private void logUnresolved() {
            var problemReport = toErrorMessage(requireNonNull(unresolved));
            logger().atWarning().log(() -> format(
                    "Unable to add a dependency on `%s` to the configuration `%s` because some " +
                            "dependencies could not be resolved: " +
                            "%s.",
                    mcJavaChecks(), configuration.getName(), problemReport
            ));
        }

        private String toErrorMessage(UnresolvedDependencyResult entry) {
            var dependency = entry.getAttempted().getDisplayName();
            var throwable = entry.getFailure();
            return format("%nDependency: `%s`%nProblem: `%s`", dependency, throwable);
        }
    }
}
