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
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;


public class GraylogBackendExtension implements AfterEachCallback, BeforeAllCallback, ParameterResolver {

    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackendExtension.class);
    private static final Namespace NAMESPACE = Namespace.create(GraylogBackendExtension.class);

    private GraylogBackend backend;
    private Lifecycle lifecycle;

    @Override
    public void beforeAll(ExtensionContext context) {

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        ApiIntegrationTest annotation = annotationFrom(context);

        lifecycle = annotation.serverLifecycle();

        Stopwatch sw = Stopwatch.createStarted();

        backend = constructBackendFrom(annotation);

        context.getStore(NAMESPACE).put(context.getRequiredTestClass().getName(), backend);

        sw.stop();

        LOG.info("Backend started after " + sw.elapsed(TimeUnit.SECONDS) + " seconds");
    }

    private GraylogBackend constructBackendFrom(ApiIntegrationTest annotation) {
        final ElasticsearchInstanceFactory factory = instantiateFactory(annotation.elasticsearchFactory());
        return GraylogBackend.createStarted(annotation.extraPorts(), factory);
    }

    private ElasticsearchInstanceFactory instantiateFactory(Class<? extends ElasticsearchInstanceFactory> factoryClass) {
        try {
            return factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to construct Elasticsearch factory: ", e);
        }
    }

    private static ApiIntegrationTest annotationFrom(ExtensionContext context) {
        Optional<Class<?>> testClass = context.getTestClass();

        if (!testClass.isPresent())
            throw new RuntimeException("Error determining test class from ExtensionContext");

        return testClass.get().getAnnotation(ApiIntegrationTest.class);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) {
            backend.printServerLog();
        }
        lifecycle.afterEach(backend);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        ImmutableSet<Class<?>> supportedTypes = ImmutableSet.of(GraylogBackend.class, RequestSpecification.class);

        return supportedTypes.contains(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> paramType = parameterContext.getParameter().getType();

        if (paramType.equals(GraylogBackend.class)) {
            return extensionContext.getStore(NAMESPACE).get(extensionContext.getRequiredTestClass().getName());
        } else if (paramType.equals(RequestSpecification.class)) {
            return requestSpec();
        }
        throw new RuntimeException("Unsupported parameter type: " + paramType);
    }

    private RequestSpecification requestSpec() {
        return new RequestSpecBuilder().build()
                .baseUri(backend.uri())
                .port(backend.apiPort())
                .basePath("/api")
                .accept(JSON)
                .contentType(JSON)
                .header("X-Requested-By", "peterchen")
                .auth().basic("admin", "admin");
    }
}
