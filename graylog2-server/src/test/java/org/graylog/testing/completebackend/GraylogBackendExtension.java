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
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.engine.support.hierarchical.ContainerMatrixHierarchicalTestExecutor;


public class GraylogBackendExtension implements ParameterResolver {
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        ImmutableSet<Class<?>> supportedTypes = ImmutableSet.of(GraylogBackend.class, RequestSpecification.class, SearchServerInstance.class, GraylogApis.class);

        return supportedTypes.contains(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> paramType = parameterContext.getParameter().getType();

        if (paramType.equals(GraylogBackend.class)) {
            return ContainerMatrixHierarchicalTestExecutor.graylogBackend.orElse(null);
        } else if (paramType.equals(RequestSpecification.class)) {
            return ContainerMatrixHierarchicalTestExecutor.requestSpecification.orElse(null);
        } else if (paramType.equals(SearchServerInstance.class)) {
            return ContainerMatrixHierarchicalTestExecutor.graylogBackend.map(GraylogBackend::searchServerInstance).orElse(null);
        } else if (paramType.equals(GraylogApis.class)) {
            if (ContainerMatrixHierarchicalTestExecutor.requestSpecification.isPresent() && ContainerMatrixHierarchicalTestExecutor.graylogBackend.isPresent()) {
                return new GraylogApis(
                        ContainerMatrixHierarchicalTestExecutor.requestSpecification.get(),
                        ContainerMatrixHierarchicalTestExecutor.graylogBackend.get()
                );
            } else {
                throw new ParameterResolutionException("Failed to obtain requestSpecification and/or graylogBackend to provide graylog APIs");
            }
        }
        throw new RuntimeException("Unsupported parameter type: " + paramType);
    }
}
