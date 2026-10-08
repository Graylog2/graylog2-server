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
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.IndexSetStatsCreator;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.function.ToLongFunction;

import static org.graylog2.metrics.entity.cache.MetricsCacheConfiguration.METRICS_CACHE_TTL_LONG;

/**
 * Cached metrics derived from the index stats of an index set, one nested class per field, which share one batched
 * stats fetch through the memo in {@link IndexSetStatsCreator}.
 */
public abstract class IndexSetStatsDescriptor extends IndexSetMetricDescriptor<Long> {
    private final String fieldName;
    private final ToLongFunction<IndexSetStats> value;
    private final IndexSetStatsCreator statsCreator;

    IndexSetStatsDescriptor(String fieldName,
                            ToLongFunction<IndexSetStats> value,
                            IndexSetStatsCreator statsCreator,
                            IndexSetService indexSetService,
                            IndexSetRegistry indexSetRegistry,
                            Duration cacheTtl) {
        super(indexSetService, indexSetRegistry, cacheTtl);
        this.fieldName = fieldName;
        this.value = value;
        this.statsCreator = statsCreator;
    }

    @Override
    public String fieldName() {
        return fieldName;
    }

    @Override
    public TypeReference<Long> cacheType() {
        return new TypeReference<>() {};
    }

    @Override
    public List<EntityMetric<Long>> compute(Collection<String> entityIds) {
        return statsCreator.getForIndexSets(indexSets(entityIds)).entrySet().stream()
                .map(entry -> new EntityMetric<>(entry.getKey(), value.applyAsLong(entry.getValue())))
                .toList();
    }

    public static final class IndexCount extends IndexSetStatsDescriptor {
        public static final String FIELD_NAME = "index_count";

        @Inject
        public IndexCount(IndexSetStatsCreator statsCreator,
                          IndexSetService indexSetService,
                          IndexSetRegistry indexSetRegistry,
                          @Named(METRICS_CACHE_TTL_LONG) Duration cacheTtl) {
            super(FIELD_NAME, IndexSetStats::indices, statsCreator, indexSetService, indexSetRegistry, cacheTtl);
        }
    }

    public static final class DocumentCount extends IndexSetStatsDescriptor {
        public static final String FIELD_NAME = "document_count";

        @Inject
        public DocumentCount(IndexSetStatsCreator statsCreator,
                             IndexSetService indexSetService,
                             IndexSetRegistry indexSetRegistry,
                             @Named(METRICS_CACHE_TTL_LONG) Duration cacheTtl) {
            super(FIELD_NAME, IndexSetStats::documents, statsCreator, indexSetService, indexSetRegistry, cacheTtl);
        }
    }

    public static final class SizeBytes extends IndexSetStatsDescriptor {
        public static final String FIELD_NAME = "size_bytes";

        @Inject
        public SizeBytes(IndexSetStatsCreator statsCreator,
                         IndexSetService indexSetService,
                         IndexSetRegistry indexSetRegistry,
                         @Named(METRICS_CACHE_TTL_LONG) Duration cacheTtl) {
            super(FIELD_NAME, IndexSetStats::size, statsCreator, indexSetService, indexSetRegistry, cacheTtl);
        }
    }
}
