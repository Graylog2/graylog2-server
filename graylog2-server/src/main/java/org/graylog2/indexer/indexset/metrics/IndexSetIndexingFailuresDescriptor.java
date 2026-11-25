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
package org.graylog2.indexer.indexset.metrics;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog2.indexer.IndexFailureCounter;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.plugin.Tools;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.graylog2.metrics.entity.cache.MetricsCacheConfiguration.METRICS_CACHE_TTL_SHORT;

/**
 * Cached {@code indexing_failures} metric, the number of messages that failed to index into each index set over the
 * last ten minutes.
 */
public class IndexSetIndexingFailuresDescriptor extends IndexSetMetricDescriptor<Long> {
    public static final String FIELD_NAME = "indexing_failures";
    static final int WINDOW_MINUTES = 10;

    private final IndexFailureCounter indexFailureCounter;

    @Inject
    public IndexSetIndexingFailuresDescriptor(IndexFailureCounter indexFailureCounter,
                                              IndexSetService indexSetService,
                                              IndexSetRegistry indexSetRegistry,
                                              @Named(METRICS_CACHE_TTL_SHORT) Duration cacheTtl) {
        super(indexSetService, indexSetRegistry, cacheTtl);
        this.indexFailureCounter = indexFailureCounter;
    }

    @Override
    public String fieldName() {
        return FIELD_NAME;
    }

    @Override
    public TypeReference<Long> cacheType() {
        return new TypeReference<>() {};
    }

    @Override
    List<EntityMetric<Long>> computeFor(Set<IndexSet> indexSets) {
        final Map<String, Long> countsByIndex = indexFailureCounter.countByIndexSince(Tools.nowUTC().minusMinutes(WINDOW_MINUTES));

        return indexSets.stream()
                .map(indexSet -> new EntityMetric<>(indexSet.getConfig().id(), countsByIndex.entrySet().stream()
                        .filter(entry -> indexSet.isManagedIndex(entry.getKey()))
                        .mapToLong(Map.Entry::getValue)
                        .sum()))
                .toList();
    }
}
