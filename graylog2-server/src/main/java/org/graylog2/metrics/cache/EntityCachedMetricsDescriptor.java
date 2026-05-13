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
import java.util.Collection;
import java.util.Map;

/**
 * Descriptor for entity metric fields that are cached in MongoDB via {@link MetricsCacheService}.
 * <p>
 * Computation ({@link #computeField}) runs without user context — the cache stores
 * the full, unfiltered result. On cache read, {@link #applyPermissionFilter} is called
 * to filter the cached value based on the current user's permissions.
 * </p>
 */
public interface EntityCachedMetricsDescriptor extends EntityMetricsDescriptor {

    /**
     * The cache TTL for this field. After this duration, the cached value is considered
     * stale and will be recomputed on the next request.
     */
    Duration cacheTtl();

    /**
     * Computes fresh values for the given entity IDs. Runs without user context —
     * the result is stored in the shared cache and must not be filtered by user permissions.
     *
     * @param entityIds the entity IDs to compute values for
     * @return map of entity ID to computed value
     */
    Map<String, Object> computeField(Collection<String> entityIds);

    /**
     * Filters a cached value based on the current user's permissions.
     * Called on every cache read to ensure users only see data they are authorized to access.
     * <p>
     * For example, a list of associated stream IDs should be filtered to only include
     * streams the user has {@code streams:read:<id>} permission for. A per-stream count
     * breakdown should be summed only for permitted streams.
     * </p>
     *
     * @param cachedValue the raw cached value (as stored by {@link #computeField})
     * @param searchUser  the current user
     * @return the permission-filtered value to return to the user
     */
    Object applyPermissionFilter(Object cachedValue, SearchUser searchUser);
}
