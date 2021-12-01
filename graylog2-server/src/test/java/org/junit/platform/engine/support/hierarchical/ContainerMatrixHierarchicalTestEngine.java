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
package org.junit.platform.engine.support.hierarchical;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.ElasticSearchInstanceFactoryByVersion;
import org.graylog.testing.completebackend.ElasticsearchInstanceFactory;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.graylog.testing.containermatrix.ContainerMatrixTestEngine;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestClassDescriptor;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestsDescriptor;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.restassured.http.ContentType.JSON;

public abstract class ContainerMatrixHierarchicalTestEngine<C extends EngineExecutionContext> implements TestEngine {
    private static final Logger LOG = LoggerFactory.getLogger(ContainerMatrixTestEngine.class);

    @Override
    public void execute(ExecutionRequest request) {
        request.getRootTestDescriptor().getChildren().forEach(descriptor -> {
            if (descriptor instanceof ContainerMatrixTestsDescriptor) {
                ContainerMatrixTestsDescriptor containerMatrixTestsDescriptor = (ContainerMatrixTestsDescriptor) descriptor;

                String esVersion = containerMatrixTestsDescriptor.getEsVersion();
                String mongoVersion = containerMatrixTestsDescriptor.getMongoVersion();
                int[] extraPorts = containerMatrixTestsDescriptor.getExtraPorts();
                List<URL> mongoDBFixtures = containerMatrixTestsDescriptor.getMongoDBFixtures();

                Class<? extends PluginJarsProvider> pluginJarsProvider = containerMatrixTestsDescriptor.getPluginJarsProvider();
                Class<? extends MavenProjectDirProvider> mavenProjectDirProvider = containerMatrixTestsDescriptor.getMavenProjectDirProvider();

                if (Lifecycle.VM.equals(containerMatrixTestsDescriptor.getLifecycle())) {
                    try (GraylogBackend backend = constructBackendFrom(esVersion, mongoVersion, extraPorts, mongoDBFixtures, pluginJarsProvider, mavenProjectDirProvider)) {
                        RequestSpecification specification = requestSpec(backend);
                        this.execute(request, ((ContainerMatrixTestsDescriptor) descriptor).getChildren(), backend, specification);
                    } catch (Exception exception) {
                        throw new JUnitException("Error executing tests for engine " + getId(), exception);
                    }
                } else if (Lifecycle.CLASS.equals(containerMatrixTestsDescriptor.getLifecycle())) {
                    for (TestDescriptor td : containerMatrixTestsDescriptor.getChildren()) {
                        List<URL> fixtures = mongoDBFixtures;
                        if (td instanceof ContainerMatrixTestClassDescriptor) {
                            fixtures = ((ContainerMatrixTestClassDescriptor) td).getMongoFixtures();
                        }
                        try (GraylogBackend backend = constructBackendFrom(esVersion, mongoVersion, extraPorts, fixtures, pluginJarsProvider, mavenProjectDirProvider)) {
                            RequestSpecification specification = requestSpec(backend);
                            this.execute(request, Collections.singleton(td), backend, specification);
                        } catch (Exception exception) {
                            throw new JUnitException("Error executing tests for engine " + getId(), exception);
                        }
                    }
                } else {
                    LOG.error("Unknown lifecycle: " + containerMatrixTestsDescriptor.getLifecycle());
                }
            } else {
                LOG.error("All children of the root should be of type 'ContainerMatrixTestsDescriptor'");
            }
            request.getEngineExecutionListener().executionFinished(descriptor, TestExecutionResult.successful());
        });
    }

    public void execute(ExecutionRequest request, Collection<? extends TestDescriptor> testDescriptors, GraylogBackend backend, RequestSpecification specification) {
        try (HierarchicalTestExecutorService executorService = createExecutorService(request)) {

            C executionContext = createExecutionContext(request);
            ThrowableCollector.Factory throwableCollectorFactory = createThrowableCollectorFactory(request);
            new ContainerMatrixHierarchicalTestExecutor<>(request, executionContext, executorService,
                    throwableCollectorFactory, testDescriptors, backend, specification).execute().get();
        } catch (Exception exception) {
            throw new JUnitException("Error executing tests for engine " + getId(), exception);
        }
    }

    private GraylogBackend constructBackendFrom(String esVersion, String mongoVersion, int[] extraPorts, List<URL> mongoDBFixtures, Class<? extends PluginJarsProvider> pluginJarsProvider, Class<? extends MavenProjectDirProvider> mavenProjectDirProvider) {
        final ElasticsearchInstanceFactory esInstanceFactory = instantiateFactory(ElasticSearchInstanceFactoryByVersion.class);
        final List<Path> pluginJars = instantiateFactory(pluginJarsProvider).getJars();
        final Path mavenProjectDir = instantiateFactory(mavenProjectDirProvider).getProjectDir();
        return GraylogBackend.createStarted(extraPorts, esVersion, mongoVersion, esInstanceFactory, pluginJars, mavenProjectDir, mongoDBFixtures);
    }

    private <T> T instantiateFactory(Class<? extends T> providerClass) {
        try {
            return providerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to construct instance of " + providerClass.getSimpleName() + ": ", e);
        }
    }

    private RequestSpecification requestSpec(GraylogBackend backend) {
        return new RequestSpecBuilder().build()
                .baseUri(backend.uri())
                .port(backend.apiPort())
                .basePath("/api")
                .accept(JSON)
                .contentType(JSON)
                .header("X-Requested-By", "peterchen")
                .auth().basic("admin", "admin");
    }

    protected HierarchicalTestExecutorService createExecutorService(ExecutionRequest request) {
        return new SameThreadHierarchicalTestExecutorService();
    }

    protected ThrowableCollector.Factory createThrowableCollectorFactory(ExecutionRequest request) {
        return OpenTest4JAwareThrowableCollector::new;
    }

    protected abstract C createExecutionContext(ExecutionRequest request);
}
