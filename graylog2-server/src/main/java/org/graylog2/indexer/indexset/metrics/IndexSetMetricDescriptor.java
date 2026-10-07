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

import com.google.common.base.Throwables;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.metrics.entity.cache.EntityCachedMetricDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for cached index set metrics, served unfiltered because the endpoint checks the read permission per ID.
 */
abstract class IndexSetMetricDescriptor<T> implements EntityCachedMetricDescriptor<T, T> {
    private static final Logger LOG = LoggerFactory.getLogger(IndexSetMetricDescriptor.class);

    private final IndexSetService indexSetService;
    private final IndexSetRegistry indexSetRegistry;
    private final Duration cacheTtl;

    IndexSetMetricDescriptor(IndexSetService indexSetService, IndexSetRegistry indexSetRegistry, Duration cacheTtl) {
        this.indexSetService = indexSetService;
        this.indexSetRegistry = indexSetRegistry;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public Duration cacheTtl() {
        return cacheTtl;
    }

    @Override
    public T computeForUser(T cachedValue, SearchUser searchUser) {
        return cachedValue;
    }

    @Override
    public final List<EntityMetric<T>> compute(Collection<String> entityIds) {
        final Set<IndexSet> indexSets = indexSets(entityIds);
        try {
            return computeFor(indexSets);
        } catch (RuntimeException e) {
            // a backend that is down leaves every value unknown instead of failing the request
            LOG.warn("Unable to compute {} for index sets {}: {}", fieldName(), entityIds, Throwables.getRootCause(e).getMessage());
            return indexSets.stream().map(indexSet -> new EntityMetric<T>(indexSet.getConfig().id(), null)).toList();
        }
    }

    abstract List<EntityMetric<T>> computeFor(Set<IndexSet> indexSets);

    /**
     * Resolves the requested index set IDs with one MongoDB query, skipping unknown and malformed IDs.
     */
    private Set<IndexSet> indexSets(Collection<String> entityIds) {
        final Set<String> validIds = entityIds.stream().filter(ObjectId::isValid).collect(Collectors.toSet());
        return indexSetRegistry.getFromIndexConfig(indexSetService.findByIds(validIds));
    }
}
