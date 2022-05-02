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

import com.google.common.io.Resources;
import org.apache.commons.lang.StringUtils;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.engine.config.CachingJupiterConfiguration;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ContainerMatrixEngineDescriptor;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestWithRunningESMongoTestsDescriptor;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestsDescriptor;
import org.junit.jupiter.engine.discovery.ContainerMatrixTestsDiscoverySelectorResolver;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.support.JupiterThrowableCollectorFactory;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.ContainerMatrixHierarchicalTestEngine;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;
import org.reflections.Reflections;

import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ContainerMatrixTestEngine extends ContainerMatrixHierarchicalTestEngine<JupiterEngineExecutionContext> {
    private static final String ENGINE_ID = "graylog-container-matrix-tests";

    @Override
    public String getId() {
        return ENGINE_ID;
    }

    private <T> Set<T> get(Set<Class<?>> annotatedClasses, Function<ContainerMatrixTestsConfiguration, Stream<T>> mapTo) {
        return annotatedClasses
                .stream()
                .map(aClass -> AnnotationSupport.findAnnotation(aClass, ContainerMatrixTestsConfiguration.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(mapTo)
                .collect(Collectors.toSet());
    }

    private Set<Class<? extends MavenProjectDirProvider>> getMavenProjectDirProvider(Set<Class<?>> annotatedClasses) {
        return get(annotatedClasses, (ContainerMatrixTestsConfiguration annotation) -> Stream.of(annotation.mavenProjectDirProvider()));
    }

    private Set<Class<? extends PluginJarsProvider>> getPluginJarsProvider(Set<Class<?>> annotatedClasses) {
        return get(annotatedClasses, (ContainerMatrixTestsConfiguration annotation) -> Stream.of(annotation.pluginJarsProvider()));
    }

    private boolean isRunningInCI() {
        String ci = System.getenv("CI");
        return !StringUtils.isBlank(ci);
    }

    private boolean enableContainerMatrixOnCI() {
        String force_matrix = System.getenv("ENABLE_CONTAINER_MATRIX_ON_CI");
        return "true".equalsIgnoreCase(force_matrix);
    }

    private Set<SearchServer> getSearchServerVersions(Set<Class<?>> annotatedClasses) {
        return get(annotatedClasses, (ContainerMatrixTestsConfiguration annotation) -> {
            if (annotation.searchVersions().length == 0) {
                return Stream.of(SearchServer.DEFAULT_VERSION);
            } if (isRunningInCI() && !enableContainerMatrixOnCI()) {
                return Stream.of(annotation.searchVersions()[0]);
            } else {
                return Stream.of(annotation.searchVersions());
            }
        });
    }

    private Set<MongodbServer> getMongoVersions(Set<Class<?>> annotatedClasses) {
        return get(annotatedClasses, (ContainerMatrixTestsConfiguration annotation) -> {
            if (annotation.mongoVersions().length == 0) {
                return Stream.of(MongodbServer.DEFAULT_VERSION);
            } if (isRunningInCI() && !enableContainerMatrixOnCI()) {
                return Stream.of(annotation.mongoVersions()[0]);
            } else {
                return Stream.of(annotation.mongoVersions());
            }
        });
    }

    private Set<Integer> getExtraPorts(Set<Class<?>> annotatedClasses) {
        return get(annotatedClasses, (ContainerMatrixTestsConfiguration annotation) -> Arrays.stream(annotation.extraPorts()).boxed());
    }

    public static List<URL> getMongoDBFixtures(Lifecycle lifecycle, Class<?> annotatedClass) {
        final List<URL> urls = new ArrayList<>();
        AnnotationSupport.findAnnotation(annotatedClass, ContainerMatrixTestsConfiguration.class).ifPresent(anno -> {
            // only aggregate, if it's VM Lifecycle
            if (anno.serverLifecycle().equals(lifecycle)) {
                final String[] fixtures = anno.mongoDBFixtures();
                Arrays.stream(fixtures).forEach(resourceName -> {
                    if (!Paths.get(resourceName).isAbsolute()) {
                        try {
                            urls.add(Resources.getResource(annotatedClass, resourceName));
                        } catch (IllegalArgumentException iae) {
                            urls.add(Resources.getResource(resourceName));
                        }
                    } else {
                        urls.add(Resources.getResource(resourceName));
                    }
                });
            }
        });
        return urls;
    }

    private List<URL> getMongoDBFixtures(Set<Class<?>> annotatedClasses) {
        final List<URL> urls = new LinkedList<>();
        for (Class<?> aClass : annotatedClasses) {
            urls.addAll(getMongoDBFixtures(Lifecycle.VM, aClass));
        }
        return urls;
    }

    private <T> T instantiateFactory(Class<? extends T> providerClass) {
        try {
            return providerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to construct instance of " + providerClass.getSimpleName() + ": ", e);
        }
    }

    protected boolean testAgainstRunningESMongoDB() {
        final String value = System.getenv("GRAYLOG_TEST_WITH_RUNNING_ES_AND_MONGODB");
        return !isBlank(value) && Boolean.parseBoolean(value);
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        JupiterConfiguration configuration = new CachingJupiterConfiguration(
                new DefaultJupiterConfiguration(discoveryRequest.getConfigurationParameters()));
        final ContainerMatrixEngineDescriptor engineDescriptor = new ContainerMatrixEngineDescriptor(uniqueId, "Graylog Container Matrix Tests", configuration);

        Reflections reflections = new Reflections("org.graylog", "org.graylog2");
        final Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(ContainerMatrixTestsConfiguration.class);
        final Set<Integer> extraPorts = getExtraPorts(annotated);
        final List<URL> mongoDBFixtures = getMongoDBFixtures(annotated);

        if (testAgainstRunningESMongoDB()) {
            // if you test from inside an IDE against running containers
            ContainerMatrixTestsDescriptor testsDescriptor = new ContainerMatrixTestWithRunningESMongoTestsDescriptor(
                    engineDescriptor,
                    extraPorts,
                    mongoDBFixtures);
            new ContainerMatrixTestsDiscoverySelectorResolver(engineDescriptor).resolveSelectors(discoveryRequest, testsDescriptor);
            engineDescriptor.addChild(testsDescriptor);
        } else {
            // for full tests, create all combinations of tests. First differentiate for maven builds.
            getMavenProjectDirProvider(annotated)
                    .forEach(mavenProjectDirProvider -> getPluginJarsProvider(annotated)
                            .forEach(pluginJarsProvider -> {
                                        MavenProjectDirProvider mpdp = instantiateFactory(mavenProjectDirProvider);
                                        PluginJarsProvider pjp = instantiateFactory(pluginJarsProvider);
                                        // now add all grouped tests for Lifecycle.VM
                                        getSearchServerVersions(annotated)
                                                .stream().map(SearchServer::getSearchVersion)
                                                .forEach(searchVersion -> getMongoVersions(annotated)
                                                        .forEach(mongoVersion -> {
                                                            ContainerMatrixTestsDescriptor testsDescriptor = new ContainerMatrixTestsDescriptor(engineDescriptor,
                                                                    Lifecycle.VM,
                                                                    mavenProjectDirProvider,
                                                                    mpdp.getUniqueId(),
                                                                    pluginJarsProvider,
                                                                    pjp.getUniqueId(),
                                                                    searchVersion,
                                                                    mongoVersion,
                                                                    extraPorts,
                                                                    mongoDBFixtures);
                                                            new ContainerMatrixTestsDiscoverySelectorResolver(engineDescriptor).resolveSelectors(discoveryRequest, testsDescriptor);
                                                            engineDescriptor.addChild(testsDescriptor);
                                                        })
                                                );
                                        // add separate test classes (Lifecycle.CLASS)
                                        getSearchServerVersions(annotated)
                                                .stream().map(SearchServer::getSearchVersion)
                                                .forEach(esVersion -> getMongoVersions(annotated)
                                                        .forEach(mongoVersion -> {
                                                            ContainerMatrixTestsDescriptor testsDescriptor = new ContainerMatrixTestsDescriptor(engineDescriptor,
                                                                    Lifecycle.CLASS,
                                                                    mavenProjectDirProvider,
                                                                    mpdp.getUniqueId(),
                                                                    pluginJarsProvider,
                                                                    pjp.getUniqueId(),
                                                                    esVersion,
                                                                    mongoVersion,
                                                                    extraPorts,
                                                                    new ArrayList<>());
                                                            new ContainerMatrixTestsDiscoverySelectorResolver(engineDescriptor).resolveSelectors(discoveryRequest, testsDescriptor);
                                                            engineDescriptor.addChild(testsDescriptor);
                                                        })
                                                );
                                    }
                            )
                    );
        }

        return engineDescriptor;
    }

    @Override
    protected HierarchicalTestExecutorService createExecutorService(ExecutionRequest request) {
        return super.createExecutorService(request);
    }

    @Override
    protected JupiterEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        return new JupiterEngineExecutionContext(request.getEngineExecutionListener(),
                getJupiterConfiguration(request));
    }

    @Override
    protected ThrowableCollector.Factory createThrowableCollectorFactory(ExecutionRequest request) {
        return JupiterThrowableCollectorFactory::createThrowableCollector;
    }

    private JupiterConfiguration getJupiterConfiguration(ExecutionRequest request) {
        ContainerMatrixEngineDescriptor engineDescriptor = (ContainerMatrixEngineDescriptor) request.getRootTestDescriptor();
        return engineDescriptor.getConfiguration();
    }
}
