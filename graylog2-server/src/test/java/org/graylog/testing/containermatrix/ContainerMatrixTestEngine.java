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
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.storage.SearchVersion;
import jakarta.annotation.Nonnull;
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

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ContainerMatrixTestEngine extends ContainerMatrixHierarchicalTestEngine<JupiterEngineExecutionContext> {
    private static final String ENGINE_ID = "graylog-container-matrix-tests";

    private static final Set<Class<?>> annotatedClasses;

    static {
        // Collect all annotated classes once to avoid the class scanning overhead on each #discover() call
        final ClassGraph classGraph = new ClassGraph()
                .ignoreClassVisibility() // JUnit5 test classes might be package-private
                .enableAnnotationInfo()
                .acceptPackages("org.graylog", "org.graylog2");
        try (final ScanResult scanResult = classGraph.scan()) {
            annotatedClasses = scanResult.getClassesWithAnnotation(ContainerMatrixTestsConfiguration.class.getCanonicalName()).stream()
                    .map(ClassInfo::loadClass)
                    .collect(Collectors.toSet());
        }
    }

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

    /**
     * Property has to be in the form of Elasticsearch|OpenSearch:x.x.x
     *
     * @return
     */
    public static Optional<SearchVersion> getSearchVersionOverride() {
        final var property = System.getProperty("graylog.matrix.tests.search.version.override");
        if(property == null) {
            return Optional.empty();
        }
        return Optional.of(SearchVersion.decode(property));
    }

    /**
     * checks if a version is compatible to the override version - if not, the test should get dropped
     *
     * @param version
     * @return
     */
    public static boolean isCompatible(SearchVersion override, SearchServer version) {
        return version.getSearchVersion().satisfies(override.distribution(), "^" + override.version().majorVersion());
    }

    private Stream<SearchVersion> filterForCompatibleVersionOrDrop(SearchServer[] versions) {
        final var optional = getSearchVersionOverride();
        if(optional.isPresent()) {
            final var override = optional.get();
            return Stream.of(versions).anyMatch(version -> isCompatible(override, version)) ? Stream.of(override) : Stream.empty();
        } else {
            return Stream.of(versions).map(SearchServer::getSearchVersion);
        }
    }

    private Map<String, String> getAdditionalConfigurationParameters(Set<Class<?>> annotatedClasses) {
        return get(annotatedClasses, (ContainerMatrixTestsConfiguration annotation) -> Arrays.stream(annotation.additionalConfigurationParameters()))
                .stream().collect(Collectors.toMap(
                        ContainerMatrixTestsConfiguration.ConfigurationParameter::key,
                        ContainerMatrixTestsConfiguration.ConfigurationParameter::value
                ));
    }

    public static List<String> getEnabledFeatureFlags(Lifecycle lifecycle, Class<?> annotatedClass) {
        return AnnotationSupport.findAnnotation(annotatedClass, ContainerMatrixTestsConfiguration.class)
                .map(annotation -> {
                    if (annotation.serverLifecycle().equals(lifecycle)) {
                        return Arrays.asList(annotation.enabledFeatureFlags());
                    } else {
                        return new ArrayList<String>();
                    }
                }).orElse(new ArrayList<>());
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
            return providerClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
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

        if (testAgainstRunningESMongoDB()) {
            // if you test from inside an IDE against running containers
            ContainerMatrixTestsDescriptor testsDescriptor = new ContainerMatrixTestWithRunningESMongoTestsDescriptor(
                    engineDescriptor,
                    getMongoDBFixtures(annotatedClasses));
            new ContainerMatrixTestsDiscoverySelectorResolver(engineDescriptor).resolveSelectors(discoveryRequest, testsDescriptor);
            engineDescriptor.addChild(testsDescriptor);
        } else {
            annotatedClasses.stream()
                    .flatMap(clazz -> testClassToDescriptors(clazz, engineDescriptor))
                    .forEach(descriptor -> {
                        new ContainerMatrixTestsDiscoverySelectorResolver(engineDescriptor).resolveSelectors(discoveryRequest, descriptor);
                        engineDescriptor.addChild(descriptor);
                    });
        }

        return engineDescriptor;
    }

    @Nonnull
    private Stream<ContainerMatrixTestsDescriptor> testClassToDescriptors(Class<?> clazz, ContainerMatrixEngineDescriptor engineDescriptor) {
        final ContainerMatrixTestsConfiguration annotation = clazz.getAnnotation(ContainerMatrixTestsConfiguration.class);
        final Class<? extends MavenProjectDirProvider> mavenProjectDirProvider = annotation.mavenProjectDirProvider();
        final Class<? extends PluginJarsProvider> pluginJarsProvider = annotation.pluginJarsProvider();
        final Lifecycle lifecycle = annotation.serverLifecycle();
        final String mavenProjectDirProviderID = instantiateFactory(mavenProjectDirProvider).getUniqueId();
        final String pluginJarsProviderID = instantiateFactory(pluginJarsProvider).getUniqueId();
        final List<URL> mongoFixtures = getMongoDBFixtures(Lifecycle.VM, clazz);
        final Map<String, String> additionalParams = getAdditionalConfigurationParameters(Collections.singleton(clazz));
        return Arrays.stream(annotation.searchVersions()).flatMap(searchServer ->
                Arrays.stream(annotation.mongoVersions()).map(mongodbServer ->
                        new ContainerMatrixTestsDescriptor(
                                engineDescriptor,
                                lifecycle,
                                mavenProjectDirProvider,
                                mavenProjectDirProviderID,
                                pluginJarsProvider,
                                pluginJarsProviderID,
                                searchServer.getSearchVersion(),
                                mongodbServer,
                                mongoFixtures,
                                Arrays.stream(annotation.enabledFeatureFlags()).collect(Collectors.toList()),
                                annotation.withMailServerEnabled(),
                                annotation.withWebhookServerEnabled(),
                                additionalParams)
                )
        ).distinct();
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
