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
package org.graylog2.configuration.tunables;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.graylog2.shared.utilities.StringUtils.f;

public final class RuntimeTunableDefaults {
    private RuntimeTunableDefaults() {
    }

    private static final ClassValue<Object> CACHE = new ClassValue<>() {
        @Override
        protected Object computeValue(Class<?> type) {
            return invoke(type);
        }
    };

    public static <T> T of(Class<T> type) {
        return type.cast(CACHE.get(type));
    }

    private static Object invoke(Class<?> type) {
        final Method method;
        try {
            method = type.getMethod("defaults");
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(f("Runtime tunable %s has no public static defaults() method", type.getCanonicalName()));
        }
        if (!Modifier.isStatic(method.getModifiers()) || !type.isAssignableFrom(method.getReturnType())) {
            throw new IllegalArgumentException(f("defaults() of runtime tunable %s must be static and return %s",
                    type.getCanonicalName(), type.getSimpleName()));
        }
        try {
            final Object defaults = method.invoke(null);
            if (defaults == null) {
                throw new IllegalArgumentException(f("defaults() of runtime tunable %s returned null", type.getCanonicalName()));
            }
            return defaults;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(f("Calling defaults() of runtime tunable %s failed", type.getCanonicalName()), e);
        }
    }
}
