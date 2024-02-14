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

import org.graylog2.Configuration;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.Set;

/**
 * Adds a safety net for class loading.
 */
@Singleton
public class SafeClasses {
    private final Set<String> prefixes;

    public static SafeClasses allGraylogInternal() {
        return new SafeClasses(Set.of("org.graylog.", "org.graylog2."));
    }

    @Inject
    public SafeClasses(@Named(Configuration.SAFE_CLASSES) @Nonnull Set<String> prefixes) {
        this.prefixes = Objects.requireNonNull(prefixes);
    }

    /**
     * Check if the class name is considered safe for loading by names from a potentially user-provided input.
     * Classes are considered safe if their fully qualified class name starts with any of the prefixes configured in
     * {@link Configuration#getSafeClasses()}.
     */
    public boolean isSafeToLoad(String className) {
        return prefixes.stream().anyMatch(className::startsWith);
    }
}
