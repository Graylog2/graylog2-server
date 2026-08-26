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
package org.graylog2.metrics.entity.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.metrics.entity.EntityMetricDescriptor;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * Descriptor for entity metric fields that are cached in MongoDB via {@link MetricsCacheService}.
 * <p>
 * Computation ({@link #compute}) runs without user context — the cache stores
 * the full, unfiltered result. On cache read, {@link #computeForUser} is called
 * to filter the cached value based on the current user's permissions.
 * </p>
 *
 * @param <C> the type of the cached value (e.g. {@code Map<String, Long>} for per-stream breakdowns)
 * @param <R> the type of the result after permission filtering (e.g. {@code Long} for a summed count)
 */
public interface EntityCachedMetricDescriptor<C, R> extends EntityMetricDescriptor {

    /**
     * The Jackson type reference for the cached value type {@code C}.
     * Used to safely deserialize values read from MongoDB, avoiding BSON type
     * coercion issues (e.g. {@code Integer} vs {@code Long}).
     */
    TypeReference<C> cacheType();

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
     * @return computed metric values per entity
     */
    List<EntityMetric<C>> compute(Collection<String> entityIds);

    /**
     * Transforms a cached value into the user-facing result, filtered by the user's permissions.
     * <p>
     * For example, a per-stream count breakdown is summed for only the streams the user
     * has {@code streams:read:<id>} permission for. A list of associated stream IDs is
     * filtered to only include permitted streams.
     * </p>
     *
     * @param cachedValue the raw cached value (as stored by {@link #compute})
     * @param searchUser  the current user
     * @return the user-facing result
     */
    R computeForUser(C cachedValue, SearchUser searchUser);
}
