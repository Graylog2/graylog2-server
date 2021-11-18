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
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.engine.support.hierarchical.ContainerMatrixHierarchicalTestExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;


public class GraylogBackendExtension implements BeforeAllCallback, ParameterResolver {

    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackendExtension.class);
    private static final Namespace NAMESPACE = Namespace.create(GraylogBackendExtension.class);

    private GraylogBackend backend;
    private Lifecycle lifecycle;

    @Override
    public void beforeAll(ExtensionContext context) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        ContainerMatrixTestsConfiguration annotation = annotationFrom(context);

        ContainerMatrixHierarchicalTestExecutor.graylogBackend.ifPresent(gb -> {
            context.getStore(NAMESPACE).put("graylogBackend", gb);
            context.getStore(NAMESPACE).put("elasticInstance", gb.elasticsearchInstance());
        });
        ContainerMatrixHierarchicalTestExecutor.requestSpecification.ifPresent(rs -> context.getStore(NAMESPACE).put("requestSpecification", rs));

        ContainerMatrixHierarchicalTestExecutor.graylogBackend.ifPresent(gb -> {
            gb.mongoDB().importFixtures(getMongoDBFixtures(context));
        });
    }

    private static List<URL> getMongoDBFixtures(ExtensionContext context) {
        final Class<?> testClass = context.getTestClass()
                .orElseThrow(() -> new IllegalStateException("Unable to get test class from extension context"));

        final String[] fixtures = testClass.getAnnotation(ContainerMatrixTestsConfiguration.class).mongoDBFixtures();
        return Arrays.stream(fixtures).map(resourceName -> {
            if (!Paths.get(resourceName).isAbsolute()) {
                try {
                    return Resources.getResource(testClass, resourceName);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return Resources.getResource(resourceName);
        }).collect(Collectors.toList());
    }

    private static ContainerMatrixTestsConfiguration annotationFrom(ExtensionContext context) {
        Optional<Class<?>> testClass = context.getTestClass();

        if (!testClass.isPresent()) {
            throw new RuntimeException("Error determining test class from ExtensionContext");
        }

        return testClass.get().getAnnotation(ContainerMatrixTestsConfiguration.class);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        ImmutableSet<Class<?>> supportedTypes = ImmutableSet.of(GraylogBackend.class, RequestSpecification.class, ElasticsearchInstance.class);

        return supportedTypes.contains(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> paramType = parameterContext.getParameter().getType();

        if (paramType.equals(GraylogBackend.class)) {
            return extensionContext.getStore(NAMESPACE).get("graylogBackend");
        } else if (paramType.equals(RequestSpecification.class)) {
            return extensionContext.getStore(NAMESPACE).get("requestSpecification");
        } else if (paramType.equals(ElasticsearchInstance.class)) {
            return extensionContext.getStore(NAMESPACE).get("elasticInstance");
        }
        throw new RuntimeException("Unsupported parameter type: " + paramType);
    }
}
