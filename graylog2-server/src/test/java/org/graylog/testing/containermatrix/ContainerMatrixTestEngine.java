package org.graylog.testing.containermatrix;

import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.engine.config.CachingJupiterConfiguration;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ContainerMatrixEngineDescriptor;
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

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ContainerMatrixTestEngine extends ContainerMatrixHierarchicalTestEngine<JupiterEngineExecutionContext> {
    private static final String ENGINE_ID = "graylog-container-matrix-tests";

    @Override
    public String getId() {
        return ENGINE_ID;
    }

    private Set<Class<? extends MavenProjectDirProvider>> getMavenProjectDirProvider(Set<Class<?>> annotatedClasses) {
        return annotatedClasses
                .stream()
                .map(aClass -> AnnotationSupport.findAnnotation(aClass, ContainerMatrixTestsConfiguration.class))
                .filter(Optional::isPresent)
                .flatMap(annotation -> Arrays.asList(annotation.get().mavenProjectDirProvider()).stream())
                .collect(Collectors.toSet());
    }

    private Set<Class<? extends PluginJarsProvider>> getPluginJarsProvider(Set<Class<?>> annotatedClasses) {
        return annotatedClasses
                .stream()
                .map(aClass -> AnnotationSupport.findAnnotation(aClass, ContainerMatrixTestsConfiguration.class))
                .filter(Optional::isPresent)
                .flatMap(annotation -> Arrays.asList(annotation.get().pluginJarsProvider()).stream())
                .collect(Collectors.toSet());
    }


    private Set<String> getEsVersions(Set<Class<?>> annotatedClasses) {
        return annotatedClasses
                .stream()
                .map(aClass -> AnnotationSupport.findAnnotation(aClass, ContainerMatrixTestsConfiguration.class))
                .filter(Optional::isPresent)
                .flatMap(annotation -> Arrays.asList(annotation.get().esVersions()).stream())
                .collect(Collectors.toSet());
    }

    private Set<String> getMongoVersions(Set<Class<?>> annotatedClasses) {
        return annotatedClasses
                .stream()
                .map(aClass -> AnnotationSupport.findAnnotation(aClass, ContainerMatrixTestsConfiguration.class))
                .filter(Optional::isPresent)
                .flatMap(annotation -> Arrays.asList(annotation.get().mongoVersions()).stream())
                .collect(Collectors.toSet());
    }

    private Set<Integer> getExtraPorts(Set<Class<?>> annotatedClasses) {
        return annotatedClasses
                .stream()
                .map(aClass -> AnnotationSupport.findAnnotation(aClass, ContainerMatrixTestsConfiguration.class))
                .filter(Optional::isPresent)
                .flatMap(annotation -> Arrays.stream(annotation.get().extraPorts()).boxed())
                .collect(Collectors.toSet());
    }

    private <T> T instantiateFactory(Class<? extends T> providerClass) {
        try {
            return providerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to construct instance of " + providerClass.getSimpleName() + ": ", e);
        }
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        JupiterConfiguration configuration = new CachingJupiterConfiguration(
                new DefaultJupiterConfiguration(discoveryRequest.getConfigurationParameters()));
        final ContainerMatrixEngineDescriptor engineDescriptor = new ContainerMatrixEngineDescriptor(uniqueId, "Graylog Container Matrix Tests", configuration);

        Reflections reflections = new Reflections("org.graylog", "org.graylog2");
        final Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(ContainerMatrixTestsConfiguration.class);
        final Set<Integer> extraPorts = getExtraPorts(annotated);

        // create all combinations of tests, first differentiate for maven builds
        getMavenProjectDirProvider(annotated)
                .forEach(mavenProjectDirProvider -> getPluginJarsProvider(annotated)
                        .forEach(pluginJarsProvider -> {
                                    MavenProjectDirProvider mpdp = instantiateFactory(mavenProjectDirProvider);
                                    PluginJarsProvider pjp = instantiateFactory(pluginJarsProvider);
                                    // now add all grouped tests for Lifecycle.VM
                                    getEsVersions(annotated)
                                            .forEach(esVersion -> getMongoVersions(annotated)
                                                    .forEach(mongoVersion -> {
                                                        ContainerMatrixTestsDescriptor testsDescriptor = new ContainerMatrixTestsDescriptor(engineDescriptor,
                                                                Lifecycle.VM,
                                                                mavenProjectDirProvider,
                                                                mpdp.getUniqueId(),
                                                                pluginJarsProvider,
                                                                pjp.getUniqueId(),
                                                                esVersion,
                                                                mongoVersion,
                                                                extraPorts);
                                                        new ContainerMatrixTestsDiscoverySelectorResolver(engineDescriptor).resolveSelectors(discoveryRequest, testsDescriptor);
                                                        engineDescriptor.addChild(testsDescriptor);
                                                    })
                                            );
                                    // add separate test classes (Lifecycle.CLASS)
                                    getEsVersions(annotated)
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
                                                                extraPorts);
                                                        new ContainerMatrixTestsDiscoverySelectorResolver(engineDescriptor).resolveSelectors(discoveryRequest, testsDescriptor);
                                                        engineDescriptor.addChild(testsDescriptor);
                                                    })
                                            );
                                }
                        )
                );

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
