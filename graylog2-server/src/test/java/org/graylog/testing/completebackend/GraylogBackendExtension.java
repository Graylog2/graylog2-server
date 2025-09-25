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

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.FactoryUtils;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.conditions.EnabledIfMongoDBCondition;
import org.graylog.testing.completebackend.conditions.EnabledIfSearchServerCondition;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraylogBackendExtension implements BeforeAllCallback, ParameterResolver, ExecutionCondition {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackendExtension.class);

    private static final Namespace NAMESPACE = Namespace.create(GraylogBackendExtension.class);
    private static final String VM_LIFECYCLE_BACKEND_KEY = "vm_lifecycle_backend";
    private static final String CLASS_LIFECYCLE_BACKEND_KEY = "class_lifecycle_backend";
    private static final String BACKEND_LIFECYCLE_KEY = "backend_lifecycle";

    private static final Set<Class<?>> SUPPORTED_PARAMETER_TYPES = ImmutableSet.of(
            SearchServerInstance.class,
            GraylogApis.class
    );

    static final String SEARCH_SERVER_DISTRIBUTION_PROPERTY = "test.integration.search-server.distribution";
    static final String SEARCH_SERVER_VERSION_PROPERTY = "test.integration.search-server.version";
    static final String MONGODB_VERSION_PROPERTY = "test.integration.mongodb.version";

    public static final boolean IMPORT_LICENSES_DEFAULT = true;

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) throws ParameterResolutionException {
        if (SUPPORTED_PARAMETER_TYPES.contains(parameterContext.getParameter().getType())) {
            if (parameterContext.getDeclaringExecutable() instanceof Constructor) {
                LOG.error(
                        "Do not use constructor injection for SearchServerInstance or GraylogApis, instead use static lifecycle methods");
                throw new ParameterResolutionException(
                        "SearchServerInstance or GraylogApis must not use constructor injection");
            }
            return true;
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) throws ParameterResolutionException {
        final var paramType = parameterContext.getParameter().getType();
        final var store = extensionContext.getStore(NAMESPACE);
        final var rootStore = extensionContext.getRoot().getStore(NAMESPACE);
        final var lifecycle = store.get(BACKEND_LIFECYCLE_KEY, Lifecycle.class);
        if (lifecycle == null) {
            throw new ParameterResolutionException("backend_lifecycle store not found");
        }

        final var backend = switch (lifecycle) {
            case VM -> rootStore.get(VM_LIFECYCLE_BACKEND_KEY, ContainerizedGraylogBackend.class);
            case CLASS -> store.get(CLASS_LIFECYCLE_BACKEND_KEY, ContainerizedGraylogBackend.class);
        };
        if (backend == null) {
            throw new ParameterResolutionException("Unable to find backend in lifecycle " + lifecycle.name() + " store");
        }
        if (paramType.equals(SearchServerInstance.class)) {
            return backend.searchServerInstance();
        } else if (paramType.equals(GraylogApis.class)) {
            return new GraylogApis(backend);
        }
        throw new RuntimeException("Unsupported parameter type: " + paramType);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final Optional<GraylogBackendConfiguration> backendConfiguration =
                AnnotationSupport.findAnnotation(context.getRequiredTestClass(), GraylogBackendConfiguration.class);
        LOG.debug("Before test: {} backend config: {}", context.getRequiredTestClass().getName(), backendConfiguration);

        final var store = context.getStore(NAMESPACE);
        final var rootStore = context.getRoot().getStore(NAMESPACE);

        if (backendConfiguration.isEmpty()) {
            LOG.warn("Extension cannot find @GraylogBackendConfiguration annotation");
            return;
        }
        final GraylogBackendConfiguration config = backendConfiguration.get();

        if (config.serverLifecycle() == Lifecycle.VM) {
            if (config.importLicenses() != IMPORT_LICENSES_DEFAULT) {
                throw new IllegalArgumentException("Changing the license import setting is not supported with VM lifecycle");
            }
            if (config.env().length > 0) {
                throw new IllegalArgumentException("Additional configuration parameters cannot be used with VM lifecycle");
            }
            if (config.enabledFeatureFlags().length > 0) {
                throw new IllegalArgumentException("Feature flags cannot be used with VM lifecycle");
            }
        }

        // remember the lifecycle, we need it when deciding which value to inject into parameters
        store.put(BACKEND_LIFECYCLE_KEY, config.serverLifecycle());

        // check if we have to create the VM lifecycle backend
        // note that ContainerizedGraylogBackend implements CloseableResource so junit will properly close the instances
        // as their contexts go out of scope.
        if (config.serverLifecycle() == Lifecycle.VM) {
            if (rootStore.get(VM_LIFECYCLE_BACKEND_KEY) == null) {
                // this backend will be re-used and only shut down at the very end
                LOG.info("Creating VM-lifecycle server backend for class {}", context.getRequiredTestClass().getName());
                final var sw = Stopwatch.createStarted();
                final var backend = createBackend(config, context.getRequiredTestClass());
                LOG.info("Created VM-lifecycle server backend for class {} in {}",
                        context.getRequiredTestClass().getName(), sw.stop().elapsed());
                rootStore.put(VM_LIFECYCLE_BACKEND_KEY, backend);
            } else {
                LOG.info("Re-using existing VM-lifecycle server backend for class {}",
                        context.getRequiredTestClass().getName());
            }
        } else if (config.serverLifecycle() == Lifecycle.CLASS) {
            // class lifecycle means we have to create a new backend
            LOG.info("Creating class-lifecycle server backend for class {}", context.getRequiredTestClass().getName());
            final var sw = Stopwatch.createStarted();
            final var backend = createBackend(config, context.getRequiredTestClass());
            LOG.info("Created class-lifecycle server backend for class {} in {}",
                    context.getRequiredTestClass().getName(), sw.stop().elapsed());
            store.put(CLASS_LIFECYCLE_BACKEND_KEY, backend);
        }
    }

    private static ContainerizedGraylogBackend createBackend(GraylogBackendConfiguration config, final Class<?> testClass) {
        final Map<String, String> env = Arrays.stream(config.env())
                .collect(Collectors.toMap(
                        GraylogBackendConfiguration.Env::key,
                        GraylogBackendConfiguration.Env::value
                ));

        return ContainerizedGraylogBackend.createStarted(
                new ContainerizedGraylogBackendServicesProvider(config.serverLifecycle()),
                BackendServiceVersions.getSearchServerVersion(),
                BackendServiceVersions.getMongoDBVersion(),
                FactoryUtils.instantiateFactory(config.pluginJarsProvider()).create(),
                FactoryUtils.instantiateFactory(config.mavenProjectDirProvider()).create(),
                List.of(config.enabledFeatureFlags()),
                config.importLicenses(),
                env,
                FactoryUtils.instantiateFactory(config.datanodePluginJarsProvider()).create()
        );
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        final Optional<GraylogBackendConfiguration> backendConfiguration =
                AnnotationSupport.findAnnotation(context.getRequiredTestClass(), GraylogBackendConfiguration.class);
        if (backendConfiguration.isEmpty()) {
            return ConditionEvaluationResult.enabled("No Graylog backend configuration found, enabling test");
        }

        return Stream.of(
                        new EnabledIfSearchServerCondition(BackendServiceVersions.getSearchServerVersion()),
                        new EnabledIfMongoDBCondition(BackendServiceVersions.getMongoDBVersion())
                )
                .map(condition -> condition.evaluateExecutionCondition(context))
                .filter(ConditionEvaluationResult::isDisabled)
                .findFirst()
                .orElse(ConditionEvaluationResult.enabled(null));
    }
}
