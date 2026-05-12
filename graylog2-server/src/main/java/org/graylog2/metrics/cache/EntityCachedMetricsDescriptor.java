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
package org.graylog2.metrics.cache;

import org.graylog.plugins.views.search.permissions.SearchUser;

import java.time.Duration;

/**
 * Extension of {@link EntityMetricsDescriptor} for fields that should be cached
 * in MongoDB via {@link MetricsCacheService}.
 * <p>
 * The cache stores the full unfiltered value. On cache read, {@link #filterValue}
 * is called to apply per-user permission filtering (e.g. filtering a list of
 * associated entity IDs by the user's per-entity read permissions).
 * </p>
 */
public interface EntityCachedMetricsDescriptor extends EntityMetricsDescriptor {

    /**
     * The cache TTL for this field. After this duration, the cached value is considered
     * stale and will be recomputed on the next request.
     */
    Duration cacheTtl();

    /**
     * Filters a cached value based on the current user's permissions.
     * Called when serving a value from the cache to ensure users only see
     * data they are authorized to access.
     * <p>
     * Default implementation returns the value as-is (no filtering).
     * Override for list-type fields that need per-entity permission filtering
     * (e.g. filtering associated stream/input IDs by read permissions).
     * </p>
     *
     * @param cachedValue the raw cached value
     * @param searchUser  the current user
     * @return the filtered value
     */
    default Object filterValue(Object cachedValue, SearchUser searchUser) {
        return cachedValue;
    }
}
