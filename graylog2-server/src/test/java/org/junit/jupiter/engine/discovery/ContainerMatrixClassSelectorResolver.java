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
package org.junit.jupiter.engine.discovery;

import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.containermatrix.ContainerMatrixTestEngine;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.containermatrix.discovery.IsContainerMatrixTestClass;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestClassDescriptor;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestsDescriptor;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsNestedTestClass;
import org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.NestedClassSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toCollection;
import static org.graylog.testing.containermatrix.discovery.IsContainerMatrixTestClass.isTestOrTestFactoryOrTestTemplateMethod;
import static org.junit.platform.commons.support.ReflectionSupport.findNestedClasses;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.commons.util.ReflectionUtils.findMethods;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.unresolved;

public class ContainerMatrixClassSelectorResolver implements SelectorResolver {

    private final IsTestClassWithTests isTestClassWithTests;
    private static final IsNestedTestClass isNestedTestClass = new IsNestedTestClass();

    private final Predicate<String> classNameFilter;
    private final JupiterConfiguration configuration;

    ContainerMatrixClassSelectorResolver(Predicate<String> classNameFilter, JupiterConfiguration configuration, final ContainerMatrixTestsDescriptor testsDescriptor) {
        this.classNameFilter = classNameFilter;
        this.configuration = configuration;
        this.isTestClassWithTests = new IsContainerMatrixTestClass(testsDescriptor);
    }

    @Override
    public Resolution resolve(ClassSelector selector, Context context) {
        Class<?> testClass = selector.getJavaClass();
        if (isTestClassWithTests.test(testClass)) {
            // Nested tests are never filtered out
            if (classNameFilter.test(testClass.getName())) {
                return toResolution(
                        context.addToParent(parent -> Optional.of(newClassTestDescriptor(parent, testClass))));
            }
        } else if (isNestedTestClass.test(testClass)) {
            return toResolution(context.addToParent(() -> DiscoverySelectors.selectClass(testClass.getEnclosingClass()),
                    parent -> Optional.of(newNestedClassTestDescriptor(parent, testClass))));
        }
        return unresolved();
    }

    @Override
    public Resolution resolve(NestedClassSelector selector, Context context) {
        if (isNestedTestClass.test(selector.getNestedClass())) {
            return toResolution(context.addToParent(() -> selectClass(selector.getEnclosingClasses()),
                    parent -> Optional.of(newNestedClassTestDescriptor(parent, selector.getNestedClass()))));
        }
        return unresolved();
    }

    @Override
    public Resolution resolve(UniqueIdSelector selector, Context context) {
        UniqueId uniqueId = selector.getUniqueId();
        UniqueId.Segment lastSegment = uniqueId.getLastSegment();
        if (ContainerMatrixTestClassDescriptor.SEGMENT_TYPE.equals(lastSegment.getType())) {
            String className = lastSegment.getValue();
            return ReflectionUtils.tryToLoadClass(className).toOptional().filter(isTestClassWithTests).map(
                    testClass -> toResolution(
                            context.addToParent(parent -> Optional.of(newClassTestDescriptor(parent, testClass))))).orElse(
                    unresolved());
        }
        if (NestedClassTestDescriptor.SEGMENT_TYPE.equals(lastSegment.getType())) {
            String simpleClassName = lastSegment.getValue();
            return toResolution(context.addToParent(() -> selectUniqueId(uniqueId.removeLastSegment()), parent -> {
                if (parent instanceof ClassBasedTestDescriptor) {
                    Class<?> parentTestClass = ((ClassBasedTestDescriptor) parent).getTestClass();
                    return ReflectionUtils.findNestedClasses(parentTestClass,
                            isNestedTestClass.and(
                                    where(Class::getSimpleName, isEqual(simpleClassName)))).stream().findFirst().flatMap(
                            testClass -> Optional.of(newNestedClassTestDescriptor(parent, testClass)));
                }
                return Optional.empty();
            }));
        }
        return unresolved();
    }

    private Optional<ContainerMatrixTestsDescriptor> findContainerMatrixTestsDescriptor(TestDescriptor parent) {
        if (parent instanceof ContainerMatrixTestsDescriptor) {
            return Optional.of((ContainerMatrixTestsDescriptor) parent);
        }

        if (parent.getParent().isPresent()) {
            return findContainerMatrixTestsDescriptor(parent.getParent().get());
        }

        return Optional.empty();
    }

    private boolean preImportLicense(Class<?> aClass) {
        Optional<ContainerMatrixTestsConfiguration> annotation = AnnotationSupport.findAnnotation(aClass, ContainerMatrixTestsConfiguration.class);
        return annotation.isPresent() ? annotation.get().preImportLicense() : ContainerMatrixTestsConfiguration.defaultPreImportLicense;
    }

    private ClassBasedTestDescriptor newClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
        Optional<ContainerMatrixTestsDescriptor> containerMatrixTestsDescriptor = findContainerMatrixTestsDescriptor(parent);

        if (containerMatrixTestsDescriptor.isPresent()) {
            final SearchVersion esVersion = containerMatrixTestsDescriptor.get().getEsVersion();
            final MongodbServer mongoVersion = containerMatrixTestsDescriptor.get().getMongoVersion();

            return new ContainerMatrixTestClassDescriptor(
                    parent,
                    testClass,
                    configuration,
                    esVersion,
                    mongoVersion,
                    ContainerMatrixTestEngine.getMongoDBFixtures(Lifecycle.CLASS, testClass),
                    preImportLicense(testClass)
            );
        } else {
            return new ContainerMatrixTestClassDescriptor(
                    parent,
                    testClass,
                    configuration,
                    ContainerMatrixTestEngine.getMongoDBFixtures(Lifecycle.CLASS, testClass),
                    preImportLicense(testClass)
            );

        }
    }

    private NestedClassTestDescriptor newNestedClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
        return new NestedClassTestDescriptor(
                parent.getUniqueId().append(NestedClassTestDescriptor.SEGMENT_TYPE, testClass.getSimpleName()), testClass,
                configuration);
    }

    private Resolution toResolution(Optional<? extends ClassBasedTestDescriptor> testDescriptor) {
        return testDescriptor.map(it -> {
            Class<?> testClass = it.getTestClass();
            List<Class<?>> testClasses = new ArrayList<>(it.getEnclosingTestClasses());
            testClasses.add(testClass);
            // @formatter:off
            return Resolution.match(Match.exact(it, () -> {
                Stream<DiscoverySelector> methods = findMethods(testClass, isTestOrTestFactoryOrTestTemplateMethod).stream()
                        .map(method -> selectMethod(testClasses, method));
                Stream<NestedClassSelector> nestedClasses = findNestedClasses(testClass, isNestedTestClass).stream()
                        .map(nestedClass -> DiscoverySelectors.selectNestedClass(testClasses, nestedClass));
                return Stream.concat(methods, nestedClasses).collect(toCollection((Supplier<Set<DiscoverySelector>>) LinkedHashSet::new));
            }));
            // @formatter:on
        }).orElse(unresolved());
    }

    private DiscoverySelector selectClass(List<Class<?>> classes) {
        if (classes.size() == 1) {
            return DiscoverySelectors.selectClass(classes.get(0));
        }
        int lastIndex = classes.size() - 1;
        return DiscoverySelectors.selectNestedClass(classes.subList(0, lastIndex), classes.get(lastIndex));
    }

    private DiscoverySelector selectMethod(List<Class<?>> classes, Method method) {
        if (classes.size() == 1) {
            return DiscoverySelectors.selectMethod(classes.get(0), method);
        }
        int lastIndex = classes.size() - 1;
        return DiscoverySelectors.selectNestedMethod(classes.subList(0, lastIndex), classes.get(lastIndex), method);
    }

}
