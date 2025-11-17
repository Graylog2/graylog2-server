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

package org.graylog.storage.opensearch3.testing;

import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class OpenSearchTestServerExtension implements ParameterResolver, AfterAllCallback {

    public static final String OPENSEARCH_INSTANCE_KEY = "OPENSEARCH_INSTANCE";

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(SearchServerInstance.class) || parameterContext.getParameter().getType().equals(OpenSearchInstance.class);
    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        final var paramType = parameterContext.getParameter().getType();
        if (paramType.equals(SearchServerInstance.class) || paramType.equals(OpenSearchInstance.class)) {

            final OpenSearchInstance instance = OpenSearchInstance.create();
            // Store it so AfterAll can retrieve it later and run cleanup
            extensionContext.getStore(ExtensionContext.Namespace.GLOBAL)
                    .put(OPENSEARCH_INSTANCE_KEY, instance);

            return instance;
        }
        throw new RuntimeException("Unsupported parameter type: " + paramType);
    }


    @Override
    public void afterAll(ExtensionContext context) {
        OpenSearchInstance value = context.getStore(ExtensionContext.Namespace.GLOBAL)
                .get(OPENSEARCH_INSTANCE_KEY, OpenSearchInstance.class);
        if (value != null) {
            value.cleanUp();
        }
    }
}
