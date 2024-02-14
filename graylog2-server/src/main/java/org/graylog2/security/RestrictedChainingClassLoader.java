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
package org.graylog2.security;

import jakarta.inject.Inject;
import org.graylog2.Configuration;
import org.graylog2.shared.plugins.ChainingClassLoader;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * A wrapper around the chaining class loader intended only for loading classes safely by considering an allow-list of
 * class name prefixes.
 */
public class RestrictedChainingClassLoader {
    private final ChainingClassLoader delegate;
    private final SafeClasses safeClasses;

    @Inject
    public RestrictedChainingClassLoader(ChainingClassLoader delegate, SafeClasses safeClasses) {
        this.delegate = delegate;
        this.safeClasses = safeClasses;
    }

    /**
     * Load the class only if the name passes the check of {@link SafeClasses#isSafeToLoad(String)}. If the class name
     * passes the check, the call is delegated to {@link ChainingClassLoader#loadClass(String)}. If it doesn't pass the
     * check, an {@link UnsafeClassLoadingAttemptException} is thrown.
     *
     * @return class as returned by the delegated call to {@link ChainingClassLoader#loadClass(String)}
     * @throws ClassNotFoundException             if the class was not found
     * @throws UnsafeClassLoadingAttemptException if the class name didn't pass the safety check of
     *                                            {@link SafeClasses#isSafeToLoad(String)}
     */
    public Class<?> loadClassSafely(String name) throws ClassNotFoundException, UnsafeClassLoadingAttemptException {
        if (safeClasses.isSafeToLoad(name)) {
            return delegate.loadClass(name);
        } else {
            throw new UnsafeClassLoadingAttemptException(
                    f("Prevented loading of unsafe class \"%s\". Consider adjusting the configuration setting " +
                            "\"%s\", if you think that this is a mistake.", name, Configuration.SAFE_CLASSES)
            );
        }
    }

}
