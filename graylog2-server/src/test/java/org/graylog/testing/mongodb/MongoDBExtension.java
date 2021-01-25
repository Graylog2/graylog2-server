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
package org.graylog.testing.mongodb;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

/**
 * Extension to provide a MongoDB service instance for JUnit 5 tests.
 *
 * <p>When used with {@link org.junit.jupiter.api.extension.ExtendWith ExtendWith} on a class or
 * {@link org.junit.jupiter.api.extension.RegisterExtension RegisterExtension} on a {@code static} field,
 * the extension starts a single MongoDB service instance for all tests in the class.
 *
 * <p>When used with {@link org.junit.jupiter.api.extension.RegisterExtension RegisterExtension} on a non-static field,
 * the extension starts a MongoDB service instance for <i>each test</i>.
 *
 * Test and setup/teardown methods can inject a {@link MongoDBTestService} parameter.
 *
 * <p>See {@link MongoDBExtensionTest} and {@link MongoDBExtensionWithRegistrationAsStaticFieldTest} for usage examples.
 */
public class MongoDBExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver, InvocationInterceptor {
    private final String version;

    private enum Lifecycle {
        ALL_TESTS, SINGLE_TEST
    }

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBExtension.class);
    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(MongoDBExtension.class);

    /**
     * Create new extension instance using the {@link MongoDBTestService#DEFAULT_VERSION default version}.
     *
     * @return the new extension instance
     */
    public static MongoDBExtension createWithDefaultVersion() {
        return new MongoDBExtension(MongoDBTestService.DEFAULT_VERSION);
    }

    public static MongoDBExtension create(String version) {
        return new MongoDBExtension(requireNonNull(version, "version cannot be null"));
    }

    // This is used by the JUnit 5 extension system
    @SuppressWarnings("unused")
    public MongoDBExtension() {
        this(MongoDBTestService.DEFAULT_VERSION);
    }

    public MongoDBExtension(String version) {
        this.version = version;
    }

    private MongoDBTestService constructInstance(ExtensionContext context, Lifecycle lifecycle) {
        if (context.getStore(NAMESPACE).get(Lifecycle.class) == null) {
            context.getStore(NAMESPACE).put(Lifecycle.class, lifecycle);
        }
        return (MongoDBTestService) context.getStore(NAMESPACE).getOrComputeIfAbsent(MongoDBTestService.class, c -> {
            LOG.debug("Starting a new MongoDB service instance with lifecycle {}", lifecycle);
            return MongoDBTestService.create(version);
        });
    }

    private void closeInstance(ExtensionContext context) {
        context.getStore(NAMESPACE).remove(Lifecycle.class);
        getInstance(context).close();
    }

    private void clearInstance(ExtensionContext context) {
        getInstance(context).dropDatabase();
    }

    private MongoDBTestService getInstance(ExtensionContext context) {
        return requireNonNull((MongoDBTestService) context.getStore(NAMESPACE).get(MongoDBTestService.class),
                "MongoDBTestService hasn't been initialized yet");
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        // When extension is used with @ExtendWith on a class or @RegisterExtension on a static field, we start a
        // single MongoDB instance for all tests in the test class
        constructInstance(context, Lifecycle.ALL_TESTS).start();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        // If there isn't an instance already, the extension has been used with @RegisterExtension on a non-static
        // field (beforeAll doesn't get called in that case), so we want to start a new MongoDB instance for each test
        // in the test class
        constructInstance(context, Lifecycle.SINGLE_TEST).start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        closeInstance(context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (context.getStore(NAMESPACE).get(Lifecycle.class) == Lifecycle.SINGLE_TEST) {
            closeInstance(context);
        } else {
            clearInstance(context);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        return MongoDBTestService.class.equals(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        if (MongoDBTestService.class.equals(parameterContext.getParameter().getType())) {
            return getInstance(context);
        }
        throw new ParameterResolutionException("Unsupported parameter type: " + parameterContext.getParameter().getName());
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext context) throws Throwable {
        processFixtures(invocationContext, context);

        invocation.proceed();
    }

    // Process MongoDBFixtures annotation on methods and classes
    private void processFixtures(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext context) {
        findFixtureAnnotation(context)
                .ifPresent(annotation -> loadFixtures(invocationContext, context, annotation));
    }

    private Optional<MongoDBFixtures> findFixtureAnnotation(ExtensionContext context) {
        ExtensionContext currentContext = context;
        Optional<MongoDBFixtures> fixtureAnnotation;

        // Find fixture annotation on current element or parent elements
        do {
            fixtureAnnotation = findAnnotation(currentContext.getElement(), MongoDBFixtures.class);

            if (!currentContext.getParent().isPresent()) {
                break;
            }

            currentContext = currentContext.getParent().get();
        } while (!fixtureAnnotation.isPresent() && currentContext != context.getRoot());
        return fixtureAnnotation;
    }

    private void loadFixtures(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext context, MongoDBFixtures fixtureAnnotation) {
        LOG.debug("Loading fixtures {} for {}#{}()",
                fixtureAnnotation.value(),
                invocationContext.getTargetClass().getCanonicalName(),
                invocationContext.getExecutable().getName());
        final MongoDBFixtureImporter fixtureImporter = new MongoDBFixtureImporter(fixtureAnnotation.value(), invocationContext.getTargetClass());
        fixtureImporter.importResources(getInstance(context).mongoDatabase());
    }
}
