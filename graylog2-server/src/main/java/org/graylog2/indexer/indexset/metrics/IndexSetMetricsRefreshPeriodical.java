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

import com.google.common.primitives.Ints;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.metrics.entity.EntityMetricDescriptor;
import org.graylog2.metrics.entity.cache.CacheResult;
import org.graylog2.metrics.entity.cache.EntityCachedMetricDescriptor;
import org.graylog2.metrics.entity.cache.MetricsCacheService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.metrics.entity.EntityMetricsModule.ENTITY_TYPE_INDEX_SETS;
import static org.graylog2.metrics.entity.cache.MetricsCacheConfiguration.METRICS_CACHE_TTL_SHORT;

/**
 * Leader-only {@link Periodical} that keeps the cached metrics of all index sets fresh by recomputing, once per short
 * cache TTL, every value that would expire before the next run.
 */
public class IndexSetMetricsRefreshPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexSetMetricsRefreshPeriodical.class);
    private static final int INITIAL_DELAY_SECONDS = 60;

    private final Map<String, EntityCachedMetricDescriptor<?, ?>> descriptorsByField;
    private final MetricsCacheService cacheService;
    private final IndexSetService indexSetService;
    private final int periodSeconds;

    @Inject
    public IndexSetMetricsRefreshPeriodical(@Named(ENTITY_TYPE_INDEX_SETS) Set<EntityMetricDescriptor> descriptors,
                                            MetricsCacheService cacheService,
                                            IndexSetService indexSetService,
                                            @Named(METRICS_CACHE_TTL_SHORT) Duration shortTtl) {
        this.descriptorsByField = descriptors.stream()
                .filter(EntityCachedMetricDescriptor.class::isInstance)
                .map(descriptor -> (EntityCachedMetricDescriptor<?, ?>) descriptor)
                .collect(Collectors.toMap(EntityMetricDescriptor::fieldName, descriptor -> descriptor));
        this.cacheService = cacheService;
        this.indexSetService = indexSetService;
        this.periodSeconds = Ints.saturatedCast(shortTtl.toSeconds());
    }

    @Override
    public void doRun() {
        final List<String> indexSetIds = indexSetService.findAll().stream().map(IndexSetConfig::id).toList();
        // a value counts as stale when it expires before the next run
        final Map<String, Duration> ttls = descriptorsByField.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().cacheTtl().minusSeconds(periodSeconds)));
        final CacheResult cacheResult = cacheService.checkCache(indexSetIds, ENTITY_TYPE_INDEX_SETS, ttls, ttls.keySet());
        descriptorsByField.forEach((field, descriptor) -> {
            final Set<String> staleIds = cacheResult.staleEntityIdsForField(field);
            if (!staleIds.isEmpty()) {
                final Map<String, Object> values = new HashMap<>();
                descriptor.compute(staleIds).forEach(metric -> values.put(metric.entityId(), metric.value()));
                cacheService.putFieldBatch(ENTITY_TYPE_INDEX_SETS, field, values);
            }
        });
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean leaderOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return INITIAL_DELAY_SECONDS;
    }

    @Override
    public int getPeriodSeconds() {
        return periodSeconds;
    }

    @Nonnull
    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
