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

import org.graylog.testing.completebackend.ContainerizedGraylogBackend;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.graylog.testing.completebackend.RunningGraylogBackend;
import org.graylog.testing.containermatrix.ContainerMatrixTestEngine;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestClassDescriptor;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestWithRunningESMongoTestsDescriptor;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestsDescriptor;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class ContainerMatrixHierarchicalTestEngine<C extends EngineExecutionContext> implements TestEngine {
    private static final Logger LOG = LoggerFactory.getLogger(ContainerMatrixTestEngine.class);

    private <T> T instantiateFactory(Class<? extends T> providerClass) {
        try {
            return providerClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Unable to construct instance of " + providerClass.getSimpleName() + ": ", e);
        }
    }

    @Override
    public void execute(ExecutionRequest request) {
        request.getRootTestDescriptor().getChildren().forEach(descriptor -> {
            if (descriptor instanceof ContainerMatrixTestWithRunningESMongoTestsDescriptor) {
                GraylogBackend backend = RunningGraylogBackend.createStarted();
                this.execute(request, descriptor.getChildren(), backend);
            } else if (descriptor instanceof ContainerMatrixTestsDescriptor) {
                ContainerMatrixTestsDescriptor containerMatrixTestsDescriptor = (ContainerMatrixTestsDescriptor) descriptor;

                SearchVersion esVersion = containerMatrixTestsDescriptor.getEsVersion();
                MongodbServer mongoVersion = containerMatrixTestsDescriptor.getMongoVersion();
                int[] extraPorts = containerMatrixTestsDescriptor.getExtraPorts();
                List<URL> mongoDBFixtures = containerMatrixTestsDescriptor.getMongoDBFixtures();
                List<String> enabledFeatureFlags = containerMatrixTestsDescriptor.getEnabledFeatureFlags();
                PluginJarsProvider pluginJarsProvider = instantiateFactory(containerMatrixTestsDescriptor.getPluginJarsProvider());
                MavenProjectDirProvider mavenProjectDirProvider = instantiateFactory(containerMatrixTestsDescriptor.getMavenProjectDirProvider());
                boolean withEnabledMailServer = containerMatrixTestsDescriptor.withEnabledMailServer();

                if (Lifecycle.VM.equals(containerMatrixTestsDescriptor.getLifecycle())) {
                    try (ContainerizedGraylogBackend backend = ContainerizedGraylogBackend.createStarted(esVersion, mongoVersion, extraPorts, mongoDBFixtures, pluginJarsProvider, mavenProjectDirProvider, enabledFeatureFlags, ContainerMatrixTestsConfiguration.defaultImportLicenses, withEnabledMailServer)) {
                        this.execute(request, descriptor.getChildren(), backend);
                    } catch (Exception exception) {
                        /* Fail hard if the containerized backend failed to start. */
                        LOG.error("Failed container startup? Error executing tests for engine " + getId(), exception);
                        System.exit(1);
//                        throw new JUnitException("Error executing tests for engine " + getId(), exception);
                    }
                } else if (Lifecycle.CLASS.equals(containerMatrixTestsDescriptor.getLifecycle())) {
                    for (TestDescriptor td : containerMatrixTestsDescriptor.getChildren()) {
                        List<URL> fixtures = mongoDBFixtures;
                        boolean preImportLicense = ContainerMatrixTestsConfiguration.defaultImportLicenses;
                        if (td instanceof ContainerMatrixTestClassDescriptor) {
                            fixtures = ((ContainerMatrixTestClassDescriptor) td).getMongoFixtures();
                            preImportLicense = ((ContainerMatrixTestClassDescriptor) td).isPreImportLicense();
                        }
                        try (ContainerizedGraylogBackend backend = ContainerizedGraylogBackend.createStarted(esVersion, mongoVersion, extraPorts, fixtures, pluginJarsProvider, mavenProjectDirProvider, enabledFeatureFlags, preImportLicense, withEnabledMailServer)) {
                            this.execute(request, Collections.singleton(td), backend);
                        } catch (Exception exception) {
                            /* Fail hard if the containerized backend failed to start. */
                            LOG.error("Failed container startup? Error executing tests for engine " + getId(), exception);
                            System.exit(1);
//                          throw new JUnitException("Error executing tests for engine " + getId(), exception);
                        }
                    }
                } else {
                    LOG.error("Unknown lifecycle: " + containerMatrixTestsDescriptor.getLifecycle());
                }
            } else {
                LOG.error("All children of the root should be of type 'ContainerMatrixTestsDescriptor' or 'ContainerMatrixTestWithRunningESMongoTestsDescriptor'");
            }
            request.getEngineExecutionListener().executionFinished(descriptor, TestExecutionResult.successful());
        });
    }

    public void execute(ExecutionRequest request, Collection<? extends TestDescriptor> testDescriptors, GraylogBackend backend) {
        try (HierarchicalTestExecutorService executorService = createExecutorService(request)) {

            C executionContext = createExecutionContext(request);
            ThrowableCollector.Factory throwableCollectorFactory = createThrowableCollectorFactory(request);
            new ContainerMatrixHierarchicalTestExecutor<>(request, executionContext, executorService,
                    throwableCollectorFactory, testDescriptors, backend).execute().get();
        } catch (Exception exception) {
            throw new JUnitException("Error executing tests for engine " + getId(), exception);
        }
    }

    protected HierarchicalTestExecutorService createExecutorService(ExecutionRequest request) {
        return new SameThreadHierarchicalTestExecutorService();
    }

    protected ThrowableCollector.Factory createThrowableCollectorFactory(ExecutionRequest request) {
        return OpenTest4JAwareThrowableCollector::new;
    }

    protected abstract C createExecutionContext(ExecutionRequest request);
}
