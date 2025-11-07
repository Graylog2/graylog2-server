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
package org.graylog.testing.elasticsearch;

import jakarta.annotation.Nonnull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit extension that checks for SkipDefaultIndexTemplate annotations on test methods and supports injecting
 * a boolean parameter into lifecycle or test methods using the same annotation.
 * This avoids exposing inner state of this extension and is a more idiomatic way to do this with the JUnit extension API.
 */
public class SkipDefaultIndexTemplateWatcher implements BeforeEachCallback, ParameterResolver {
    private static final Namespace NAMESPACE = Namespace.create(SkipDefaultIndexTemplateWatcher.class);

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        boolean hasAnnotation = context.getRequiredTestMethod().isAnnotationPresent(SkipDefaultIndexTemplate.class);
        context.getStore(NAMESPACE).put(context.getUniqueId(), hasAnnotation);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, @Nonnull ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == boolean.class
                && parameterContext.isAnnotated(SkipDefaultIndexTemplate.class);
    }

    @Override
    public @Nullable Object resolveParameter(@Nonnull ParameterContext parameterContext, @Nonnull ExtensionContext extensionContext) throws ParameterResolutionException {
        return extensionContext.getStore(NAMESPACE)
                .getOrDefault(extensionContext.getUniqueId(), Boolean.class, false);
    }
}
