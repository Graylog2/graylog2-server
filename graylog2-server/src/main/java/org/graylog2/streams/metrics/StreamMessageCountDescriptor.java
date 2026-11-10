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
package org.graylog2.streams.metrics;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.events.search.MoreSearch;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.metrics.entity.cache.EntityCachedMetricDescriptor;
import org.graylog2.metrics.entity.cache.MetricsCacheConfiguration;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog2.plugin.Message.FIELD_STREAMS;

/**
 * Provides {@code message_count} metrics for streams.
 * Counts messages per stream over the last 24h using a terms aggregation on the {@code streams} field.
 */
public class StreamMessageCountDescriptor implements EntityCachedMetricDescriptor<Long, Long> {

    public static final String FIELD_NAME = "message_count";

    private final MoreSearch moreSearch;
    private final Duration cacheTtl;

    @Inject
    public StreamMessageCountDescriptor(MoreSearch moreSearch,
                                        @Named(MetricsCacheConfiguration.METRICS_CACHE_TTL_LONG) Duration cacheTtl) {
        this.moreSearch = moreSearch;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public TypeReference<Long> cacheType() {
        return new TypeReference<>() {};
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
    public List<EntityMetric<Long>> compute(Collection<String> entityIds) {
        final Map<String, Long> counts = moreSearch.aggregateTerms(
                entityIds.stream()
                        .map(id -> FIELD_STREAMS + ":" + MoreSearch.luceneEscape(id))
                        .collect(Collectors.joining(" OR ")),
                RelativeRange.create(MetricsCacheConfiguration.RANGE_SECONDS_24H),
                FIELD_STREAMS, entityIds.size(), entityIds);

        return entityIds.stream()
                .map(id -> new EntityMetric<>(id, counts.getOrDefault(id, 0L)))
                .toList();
    }

    @Override
    public Long computeForUser(Long cachedValue, SearchUser searchUser) {
        return cachedValue;
    }
}
