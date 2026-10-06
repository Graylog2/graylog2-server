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
import com.google.common.base.Throwables;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.metrics.entity.EntityMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import static org.graylog2.metrics.entity.cache.MetricsCacheConfiguration.METRICS_CACHE_TTL_SHORT;

/**
 * Cached {@code deflector_health} metric, a stub on the min-reduced {@link ClusterAdapter#deflectorHealth(Collection)}
 * call that gives every writable index set the worst health among them until a per-alias lookup exists.
 */
public class IndexSetDeflectorHealthDescriptor extends IndexSetMetricDescriptor<HealthStatus> {
    private static final Logger LOG = LoggerFactory.getLogger(IndexSetDeflectorHealthDescriptor.class);

    public static final String FIELD_NAME = "deflector_health";

    private final ClusterAdapter clusterAdapter;

    @Inject
    public IndexSetDeflectorHealthDescriptor(ClusterAdapter clusterAdapter,
                                             IndexSetService indexSetService,
                                             IndexSetRegistry indexSetRegistry,
                                             @Named(METRICS_CACHE_TTL_SHORT) Duration cacheTtl) {
        super(indexSetService, indexSetRegistry, cacheTtl);
        this.clusterAdapter = clusterAdapter;
    }

    @Override
    public String fieldName() {
        return FIELD_NAME;
    }

    @Override
    public TypeReference<HealthStatus> cacheType() {
        return new TypeReference<>() {};
    }

    @Override
    public List<EntityMetric<HealthStatus>> compute(Collection<String> entityIds) {
        final Set<IndexSet> indexSets = indexSets(entityIds);
        final List<String> writeAliases = indexSets.stream()
                .filter(indexSet -> indexSet.getConfig().isWritable())
                .map(IndexSet::getWriteIndexAlias)
                .toList();
        final HealthStatus health = writeAliases.isEmpty() ? null : deflectorHealth(writeAliases);

        return indexSets.stream()
                .map(indexSet -> new EntityMetric<>(indexSet.getConfig().id(), indexSet.getConfig().isWritable() ? health : null))
                .toList();
    }

    // a failed lookup, for example a write alias with several targets, leaves the health unknown instead of failing the request
    @Nullable
    private HealthStatus deflectorHealth(List<String> writeAliases) {
        try {
            return clusterAdapter.deflectorHealth(writeAliases).orElse(null);
        } catch (RuntimeException e) {
            LOG.warn("Unable to determine the deflector health of {}: {}", writeAliases, Throwables.getRootCause(e).getMessage());
            return null;
        }
    }
}
