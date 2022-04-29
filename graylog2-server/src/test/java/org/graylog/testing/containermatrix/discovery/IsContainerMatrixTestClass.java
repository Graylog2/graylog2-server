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
package org.graylog.testing.containermatrix.discovery;

import com.google.common.collect.Sets;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestWithRunningESMongoTestsDescriptor;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestsDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsContainerMatrixTest;
import org.junit.jupiter.engine.discovery.predicates.IsNestedTestClass;
import org.junit.jupiter.engine.discovery.predicates.IsPotentialTestContainer;
import org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IsContainerMatrixTestClass extends IsTestClassWithTests {
    private static final IsContainerMatrixTest isTestMethod = new IsContainerMatrixTest();

    private static final IsPotentialTestContainer isPotentialTestContainer = new IsPotentialTestContainer();

    private static final IsNestedTestClass isNestedTestClass = new IsNestedTestClass();

    private boolean hasNestedTests(Class<?> candidate) {
        return !ReflectionUtils.findNestedClasses(candidate, isNestedTestClass).isEmpty();
    }

    public static final Predicate<Method> isTestOrTestFactoryOrTestTemplateMethod = isTestMethod;

    private final ContainerMatrixTestsDescriptor container;

    public IsContainerMatrixTestClass(final ContainerMatrixTestsDescriptor container) {
        this.container = container;
    }

    private boolean matchAnnotationToContainer(Class<?> candidate) {
        Optional<ContainerMatrixTestsConfiguration> annotation = AnnotationSupport.findAnnotation(candidate, ContainerMatrixTestsConfiguration.class);
        if (annotation.isPresent()) {
            ContainerMatrixTestsConfiguration config = annotation.get();
            if (container instanceof ContainerMatrixTestWithRunningESMongoTestsDescriptor) {
                return true;
            } else {
                return config.serverLifecycle().equals(container.getLifecycle())
                        && config.mavenProjectDirProvider().equals(container.getMavenProjectDirProvider())
                        && config.pluginJarsProvider().equals(container.getPluginJarsProvider())
                        && getSearchServers(config).contains(container.getEsVersion())
                        && getMongodbServers(config).contains(container.getMongoVersion());
            }
        } else {
            // Annotation should be present!
            return false;
        }
    }

    private Set<MongodbServer> getMongodbServers(ContainerMatrixTestsConfiguration config) {
        return Sets.newHashSet(config.mongoVersions());
    }

    private Set<SearchVersion> getSearchServers(ContainerMatrixTestsConfiguration config) {
        return Stream.of(config.searchVersions()).map(SearchServer::getSearchVersion).collect(Collectors.toSet());
    }

    @Override
    public boolean test(Class<?> candidate) {
        if (AnnotationSupport.isAnnotated(candidate, ContainerMatrixTestsConfiguration.class)) {
            boolean annotationMatchesContext = matchAnnotationToContainer(candidate);
            boolean isContainer = isPotentialTestContainer.test(candidate);
            boolean hasTestMethod = hasTestOrTestFactoryOrTestTemplateMethods(candidate);
            return isContainer && annotationMatchesContext && (hasTestMethod || hasNestedTests(candidate));
        } else {
            return false;
        }
    }

    private boolean hasTestOrTestFactoryOrTestTemplateMethods(Class<?> candidate) {
        return ReflectionUtils.isMethodPresent(candidate, isTestOrTestFactoryOrTestTemplateMethod);
    }

}
