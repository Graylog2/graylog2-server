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
package org.graylog2.metrics;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.events.search.MoreSearch;
import org.graylog.events.search.SourceStreamFilter;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.metrics.cache.EntityCachedMetricsDescriptor;
import org.graylog2.metrics.cache.MetricsCacheConfiguration;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;


public class InputMessageCountDescriptor implements EntityCachedMetricsDescriptor {

    public static final String FIELD_NAME = "message_count";

    private static final int MESSAGE_COUNT_RANGE_SECONDS = 86400; // 24h

    private final MoreSearch moreSearch;
    private final Duration cacheTtl;

    @Inject
    public InputMessageCountDescriptor(MoreSearch moreSearch,
                                       @Named(MetricsCacheConfiguration.METRICS_CACHE_TTL_LONG) Duration cacheTtl) {
        this.moreSearch = moreSearch;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public String entityType() {
        return MetricsModule.ENTITY_TYPE_INPUTS;
    }

    @Override
    public String fieldName() {
        return FIELD_NAME;
    }

    @Override
    public Duration cacheTtl() {
        return cacheTtl;
    }

    @Override
    public Map<String, Object> computeField(Collection<String> entityIds) {
        final String queryString = entityIds.stream()
                .map(id -> FIELD_GL2_SOURCE_INPUT + ":" + id)
                .collect(Collectors.joining(" OR "));

        final Map<String, Map<String, Long>> grouped = moreSearch.aggregateGroupedTerms(
                queryString,
                RelativeRange.create(MESSAGE_COUNT_RANGE_SECONDS),
                SourceStreamFilter.allAllowed(),
                FIELD_GL2_SOURCE_INPUT, "streams",
                entityIds.size(), Integer.MAX_VALUE);

        // Store per-stream breakdown as the cached value
        final Map<String, Object> result = new HashMap<>();
        for (final String entityId : entityIds) {
            result.put(entityId, grouped.getOrDefault(entityId, Map.of()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object applyPermissionFilter(Object cachedValue, SearchUser searchUser) {
        final Map<String, Long> countsByStream = (Map<String, Long>) cachedValue;
        return countsByStream.entrySet().stream()
                .filter(e -> searchUser.canReadStream(e.getKey()))
                .mapToLong(Map.Entry::getValue)
                .sum();
    }
}
