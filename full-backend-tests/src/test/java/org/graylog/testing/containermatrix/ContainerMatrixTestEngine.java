/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.testing.containermatrix;

import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.containermatrix.descriptors.ContainerMatrixEngineDescriptor;
import org.graylog.testing.containermatrix.descriptors.ContainerMatrixTestClassDescriptor;
import org.graylog.testing.containermatrix.descriptors.ContainerMatrixTestsDescriptor;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.graylog2.shared.utilities.StringUtils.f;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

public class ContainerMatrixTestEngine implements TestEngine {
    private static final Logger LOG = LoggerFactory.getLogger(ContainerMatrixTestEngine.class);

    private static final String ENGINE_ID = "graylog-container-matrix-tests";

    public static ConditionEvaluationResult evaluate(DisabledIfEnvironmentVariable annotation) {
        String name = annotation.named().trim();
        String regex = annotation.matches();
        Preconditions.notBlank(name, () -> "The 'named' attribute must not be blank in " + annotation);
        Preconditions.notBlank(regex, () -> "The 'matches' attribute must not be blank in " + annotation);
        String actual = getEnvironmentVariable(name);

        // Nothing to match against?
        if (actual == null) {
            return enabled(f("Environment variable [%s] does not exist", name));
        }

        if (actual.matches(regex)) {
            return disabled(f("Environment variable [%s] with value [%s] matches regular expression [%s]", name,
                    actual, regex), annotation.disabledReason());
        }
        // else
        return enabled(f("Environment variable [%s] with value [%s] does not match regular expression [%s]", name,
                actual, regex));
    }

    public static ConditionEvaluationResult evaluate(EnabledIfEnvironmentVariable annotation) {

        String name = annotation.named().trim();
        String regex = annotation.matches();
        Preconditions.notBlank(name, () -> "The 'named' attribute must not be blank in " + annotation);
        Preconditions.notBlank(regex, () -> "The 'matches' attribute must not be blank in " + annotation);
        String actual = getEnvironmentVariable(name);

        // Nothing to match against?
        if (actual == null) {
            return disabled(f("Environment variable [%s] does not exist", name), annotation.disabledReason());
        }
        if (actual.matches(regex)) {
            return enabled(f("Environment variable [%s] with value [%s] matches regular expression [%s]", name,
                    actual, regex));
        }
        return disabled(f("Environment variable [%s] with value [%s] does not match regular expression [%s]", name,
                actual, regex), annotation.disabledReason());
    }

    /**
     * Get the value of the named environment variable.
     *
     * <p>The default implementation simply delegates to
     * {@link System#getenv(String)}. Can be overridden in a subclass for
     * testing purposes.
     */
    protected static String getEnvironmentVariable(String name) {
        return System.getenv(name);
    }

    private static final Predicate<Class<?>> IS_CANDIDATE = classCandidate -> {
        if (ReflectionUtils.isAbstract(classCandidate)) {
            return false;
        }
        if (ReflectionUtils.isPrivate(classCandidate)) {
            return false;
        }
        if (AnnotationSupport.isAnnotated(classCandidate, ContainerMatrixTestsConfiguration.class)) {
            if (AnnotationSupport.isAnnotated(classCandidate, EnabledIfEnvironmentVariable.class)) {
                return AnnotationSupport
                        .findAnnotation(classCandidate, EnabledIfEnvironmentVariable.class).map(a -> !evaluate(a).isDisabled())
                        .orElseThrow(() -> new RuntimeException("Annotation should exist - it has been checked for the given class before..."));
            }
            if (AnnotationSupport.isAnnotated(classCandidate, DisabledIfEnvironmentVariable.class)) {
                return AnnotationSupport
                        .findAnnotation(classCandidate, DisabledIfEnvironmentVariable.class).map(a -> !evaluate(a).isDisabled())
                        .orElseThrow(() -> new RuntimeException("Annotation should exist - it has been checked for the given class before..."));
            }
            return true;
        }
        return false;
    };

    @Override
    public String getId() {
        return ENGINE_ID;
    }

    @Override
    public TestDescriptor discover(final EngineDiscoveryRequest request, final UniqueId uniqueId) {
        final ContainerMatrixEngineDescriptor engineDescriptor = new ContainerMatrixEngineDescriptor(uniqueId, "Graylog Container Matrix Tests");

        // ClasspathRootSelector used by IntelliJ when starting tests via the GUI
        request.getSelectorsByType(ClasspathRootSelector.class).forEach(selector -> appendTestsInClasspath(request, selector, engineDescriptor));
        // PackageSelector not used by IntelliJ, but left in because it was in the example that I used to bootstrap this code
        request.getSelectorsByType(PackageSelector.class).forEach(selector -> appendTestsInPackage(selector.getPackageName(), engineDescriptor));
        // ClassSelector used by IntelliJ when starting tests via the GUI
        request.getSelectorsByType(ClassSelector.class).forEach(selector -> appendTestsInClass(selector.getJavaClass(), engineDescriptor));

        return engineDescriptor;
    }

    private void appendTestsInClasspath(final EngineDiscoveryRequest request, final ClasspathRootSelector classpathRootSelector, final ContainerMatrixEngineDescriptor engineDescriptor) {
        final List<Predicate<String>> allPredicates = new ArrayList<>();
        // only ClassNameFilter is used by IntelliJ, maybe PackageNameFilter is used by other IDEs? The following line is only left in as an example
        // request.getFiltersByType(PackageNameFilter.class).forEach(filter -> allPredicates.add(filter.toPredicate()));
        request.getFiltersByType(ClassNameFilter.class).forEach(filter -> allPredicates.add(filter.toPredicate()));
        Predicate<String> combined = allPredicates.stream().reduce(x -> true, Predicate::and);

        ReflectionSupport.findAllClassesInClasspathRoot(classpathRootSelector.getClasspathRoot(), IS_CANDIDATE, combined)
                .forEach(aClass -> appendTestsInClass(aClass, engineDescriptor));
    }

    private void appendTestsInPackage(final String packageName, final ContainerMatrixEngineDescriptor engineDescriptor) {
        ReflectionSupport.findAllClassesInPackage(packageName, IS_CANDIDATE, name -> true)
                .forEach(aClass -> appendTestsInClass(aClass, engineDescriptor));
    }

    private void appendTestsInClass(final Class<?> javaClass, final ContainerMatrixEngineDescriptor engineDescriptor) {
        if (IS_CANDIDATE.test(javaClass)) {
            // Annotation must exist here -> IS_CANDIDATE predicate checks this
            ContainerMatrixTestsConfiguration configuration = AnnotationSupport
                    .findAnnotation(javaClass, ContainerMatrixTestsConfiguration.class)
                    .orElseThrow(() -> new RuntimeException("Annotation should exist - it has been checked for the given class before..."));
            // convert to set to make sure that versions are unique and not listed multiple times, use LinkedHashSet to preserve order
            Set<String> esVersions = new LinkedHashSet<>(Arrays.asList(configuration.esVersions()));
            Set<String> mongoVersions = new LinkedHashSet<>(Arrays.asList(configuration.mongoVersions()));

            ContainerMatrixTestsDescriptor containerMatrixTestsDescriptor = findOrCreateDescriptor(engineDescriptor, configuration);
            containerMatrixTestsDescriptor.addExtraPorts(configuration.extraPorts());
            esVersions.forEach(esVersion -> mongoVersions.forEach(mongoVersion ->
                    containerMatrixTestsDescriptor.addChild(new ContainerMatrixTestClassDescriptor(javaClass, engineDescriptor, esVersion, mongoVersion))
            ));
        }
    }

    private ContainerMatrixTestsDescriptor createNewDescriptor(ContainerMatrixEngineDescriptor engineDescriptor, ContainerMatrixTestsConfiguration configuration) {
        ContainerMatrixTestsDescriptor containerMatrixTestsDescriptor = new ContainerMatrixTestsDescriptor(engineDescriptor, configuration.serverLifecycle(), configuration.mavenProjectDirProvider(), configuration.pluginJarsProvider());
        engineDescriptor.addChild(containerMatrixTestsDescriptor);
        return containerMatrixTestsDescriptor;
    }

    private ContainerMatrixTestsDescriptor findOrCreateDescriptor(final ContainerMatrixEngineDescriptor engineDescriptor, final ContainerMatrixTestsConfiguration configuration) {
        // for the given configuration, reuse/add to an existing descriptor
        return (ContainerMatrixTestsDescriptor) engineDescriptor
                .getChildren()
                .stream()
                .filter(child -> child instanceof ContainerMatrixTestsDescriptor)
                .map(child -> (ContainerMatrixTestsDescriptor) child)
                .filter(child -> child.is(configuration))
                .findFirst()
                .orElse(createNewDescriptor(engineDescriptor, configuration));
    }

    @Override
    public void execute(ExecutionRequest request) {
        TestDescriptor root = request.getRootTestDescriptor();
        new ContainerMatrixTestExecutor().execute(request, root);
    }
}
