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
import org.graylog.events.search.MoreSearchAdapter;
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

import static org.graylog2.plugin.Message.FIELD_GL2_PROCESSING_DURATION_MS;
import static org.graylog2.plugin.Message.FIELD_STREAMS;

/**
 * Provides {@code avg_processing_time_ms} metrics for streams.
 * Computes the average {@code gl2_processing_duration_ms} per stream over the last 15 minutes.
 */
public class StreamAvgProcessingTimeDescriptor implements EntityCachedMetricDescriptor<Double, Double> {

    public static final String FIELD_NAME = "avg_processing_time_ms";

    private final MoreSearch moreSearch;
    private final Duration cacheTtl;

    @Inject
    public StreamAvgProcessingTimeDescriptor(MoreSearch moreSearch,
                                             @Named(MetricsCacheConfiguration.METRICS_CACHE_TTL_SHORT) Duration cacheTtl) {
        this.moreSearch = moreSearch;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public TypeReference<Double> cacheType() {
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
    public List<EntityMetric<Double>> compute(Collection<String> entityIds) {
        final Map<String, Double> results = moreSearch.aggregateGroupedMetric(
                entityIds.stream()
                        .map(id -> FIELD_STREAMS + ":" + MoreSearch.luceneEscape(id))
                        .collect(Collectors.joining(" OR ")),
                RelativeRange.create(MetricsCacheConfiguration.RANGE_SECONDS_15M),
                FIELD_STREAMS, MoreSearchAdapter.AggregationType.AVG, FIELD_GL2_PROCESSING_DURATION_MS,
                entityIds.size(), entityIds);

        return entityIds.stream()
                .map(id -> new EntityMetric<>(id, results.getOrDefault(id, 0.0)))
                .toList();
    }

    @Override
    public Double computeForUser(Double cachedValue, SearchUser searchUser) {
        return cachedValue;
    }
}
