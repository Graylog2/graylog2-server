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
package org.graylog2.shared.utilities;

/**
 * Utility methods related to Google AutoValue
 */
public final class AutoValueUtils {
    private AutoValueUtils() {
    }

    /**
     * Get the canonical class name of the provided {@link Class} with special handling of Google AutoValue classes.
     *
     * @param aClass a class
     * @return the canonical class name of {@code aClass} or its super class in case of an auto-generated class by
     * Google AutoValue
     * @see Class#getCanonicalName()
     * @see com.google.auto.value.AutoValue
     */
    public static String getCanonicalName(final Class<?> aClass) {
        Class<?> cls = aClass;
        while (cls.getSimpleName().matches("^\\$*AutoValue_.*")) {
            cls = cls.getSuperclass();
        }

        return cls.getCanonicalName();
    }
}
