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
package org.graylog.testing.completebackend;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.FactoryUtils;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.GraylogBackendConfiguration;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GraylogBackendExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackendExtension.class);

    private static final Namespace NAMESPACE = Namespace.create(GraylogBackendExtension.class);
    public static final String VM_LIFECYCLE_BACKEND_KEY = "vm_lifecycle_backend";
    public static final String CLASS_LIFECYCLE_BACKEND_KEY = "class_lifecycle_backend";
    public static final String BACKEND_LIFECYCLE_KEY = "backend_lifecycle";

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        ImmutableSet<Class<?>> supportedTypes = ImmutableSet.of(SearchServerInstance.class, GraylogApis.class);

        if (supportedTypes.contains(parameterContext.getParameter().getType())) {
            if (parameterContext.getDeclaringExecutable() instanceof Constructor) {
                LOG.error("Do not use constructor injection for SearchServerInstance or GraylogApis, instead use static lifecycle methods");
                throw new ParameterResolutionException("SearchServerInstance or GraylogApis must not use constructor injection");
            }
            return true;
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> paramType = parameterContext.getParameter().getType();
        final var store = extensionContext.getStore(NAMESPACE);
        final var lifecycle = store.get(BACKEND_LIFECYCLE_KEY, Lifecycle.class);
        if (lifecycle == null) {
            throw new ParameterResolutionException("backend_lifecycle store not found");
        }

        final var backend = switch (lifecycle) {
            case VM ->
                    extensionContext.getRoot().getStore(NAMESPACE).get(VM_LIFECYCLE_BACKEND_KEY, ContainerizedGraylogBackend.class);
            case CLASS ->
                    extensionContext.getRoot().getStore(NAMESPACE).get(CLASS_LIFECYCLE_BACKEND_KEY, ContainerizedGraylogBackend.class);
        };
        if (paramType.equals(SearchServerInstance.class)) {
            if (backend != null) {
                return backend.searchServerInstance();
            }
            throw new ParameterResolutionException("No Containerized Graylog backend found, cannot return search server instance");
        } else if (paramType.equals(GraylogApis.class)) {
            if (backend != null) {
                return new GraylogApis(backend);
            }
            throw new ParameterResolutionException("No Containerized Graylog backend found, cannot return graylog apis instance");
        }
        throw new RuntimeException("Unsupported parameter type: " + paramType);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final Optional<GraylogBackendConfiguration> backendConfiguration =
                AnnotationSupport.findAnnotation(context.getRequiredTestClass(), GraylogBackendConfiguration.class);
        LOG.info("Before test: {} backend config: {}", context.getRequiredTestClass().getName(), backendConfiguration);
        final var store = context.getStore(NAMESPACE);
        final var rootStore = context.getRoot().getStore(NAMESPACE);

        if (backendConfiguration.isEmpty()) {
            LOG.warn("Extension cannot find @GraylogBackendConfiguration annotation");
            return;
        }
        final GraylogBackendConfiguration config = backendConfiguration.get();
        // remember the lifecycle, we need it when deciding which value to inject into parameters
        store.put(BACKEND_LIFECYCLE_KEY, config.serverLifecycle());

        // TODO check if we have to run fixture imports on the new backend somehow
        // we should push all of them out of the annotation and into the test lifecycle

        // check if we have to create the VM lifecycle backend
        if (config.serverLifecycle() == Lifecycle.VM && rootStore.get(VM_LIFECYCLE_BACKEND_KEY) == null) {
            // this backend will be re-used and only shut down at the very end
            final ContainerizedGraylogBackend graylogBackend = createBackend(config);
            LOG.info("Created VM lifecycle Graylog backend: {}", graylogBackend);
            rootStore.put(VM_LIFECYCLE_BACKEND_KEY, graylogBackend);
        } else if (config.serverLifecycle() == Lifecycle.CLASS) {
            // class lifecycle means we have to create a new backend
            var graylogBackend = createBackend(config);
            LOG.info("Creating class lifecyle graylog backend: {}", graylogBackend);
            store.put(CLASS_LIFECYCLE_BACKEND_KEY, graylogBackend);
        }
    }

    private static ContainerizedGraylogBackend createBackend(GraylogBackendConfiguration config) {
        // TODO make overridable
        SearchVersion searchVersion = SearchServer.DATANODE_DEV.getSearchVersion();
        MongodbServer mongoVersion = MongodbServer.DEFAULT_VERSION;
        // List<URL> mongoDBFixtures = config.getMongoDBFixtures();
        // List<String> enabledFeatureFlags = config.getEnabledFeatureFlags();
        PluginJarsProvider pluginJarsProvider = FactoryUtils.instantiateFactory(config.pluginJarsProvider()).create();
        PluginJarsProvider datanodePluginJarsProvider = FactoryUtils.instantiateFactory(config.datanodePluginJarsProvider()).create();
        MavenProjectDirProvider mavenProjectDirProvider = FactoryUtils.instantiateFactory(config.mavenProjectDirProvider()).create();
        boolean withEnabledMailServer = config.withMailServerEnabled();
        boolean withEnabledWebhookServer = config.withWebhookServerEnabled();
        final Map<String, String> configParams = Arrays.stream(config.additionalConfigurationParameters()).collect(Collectors.toMap(
                GraylogBackendConfiguration.ConfigurationParameter::key,
                GraylogBackendConfiguration.ConfigurationParameter::value
        ));

        return ContainerizedGraylogBackend.createStarted(
                new ContainerizedGraylogBackendServicesProvider(),
                searchVersion,
                mongoVersion,
                List.of(),
                pluginJarsProvider,
                mavenProjectDirProvider,
                List.of(),
                config.importLicenses(),
                withEnabledMailServer,
                withEnabledWebhookServer,
                configParams,
                datanodePluginJarsProvider
        );
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        final Class<?> testClass = context
                .getTestClass()
                .orElseThrow(() -> new ExtensionConfigurationException("GraylogBackendExtension only supports classes"));

        // remove the backend from the local store and close it. this way any subsequent use will fail early
        // and the backend won't be in a usable state anyway
        final ContainerizedGraylogBackend backend = context.getStore(NAMESPACE).remove(CLASS_LIFECYCLE_BACKEND_KEY, ContainerizedGraylogBackend.class);
        if (backend != null) {
            LOG.info("Closing backend for: {}", testClass.getName());
            backend.close();
        }
    }
}
