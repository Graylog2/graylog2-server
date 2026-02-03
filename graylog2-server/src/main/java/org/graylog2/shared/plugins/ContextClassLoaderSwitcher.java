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
package org.graylog2.shared.plugins;

import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Context class loader utils.
 */
public final class ContextClassLoaderSwitcher {
    private ContextClassLoaderSwitcher() {
    }

    /**
     * Executes the given callback with the context class loader set to the classloader of the given class.
     *
     * @param contextClass the class that references the desired context class loader
     * @param callback     the callback to execute
     * @param <T>          the callback's return value type
     * @return the callback's return value
     */
    public static <T> T executeWithContextClassLoader(@Nonnull Class<?> contextClass, @Nonnull Supplier<T> callback) {
        requireNonNull(contextClass, "contextClass can't be null");
        requireNonNull(callback, "callback can't be null");

        final var originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new LoggingClassLoader(contextClass.getClassLoader()));
            return callback.get();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
