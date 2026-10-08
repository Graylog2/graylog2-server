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

import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.metrics.entity.cache.EntityCachedMetricDescriptor;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for cached index set metrics, served unfiltered because the endpoint checks the read permission per ID.
 */
abstract class IndexSetMetricDescriptor<T> implements EntityCachedMetricDescriptor<T, T> {
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

    /**
     * Resolves the requested index set IDs with one MongoDB query, skipping unknown and malformed IDs.
     */
    Set<IndexSet> indexSets(Collection<String> entityIds) {
        final Set<String> validIds = entityIds.stream().filter(ObjectId::isValid).collect(Collectors.toSet());
        return indexSetRegistry.getFromIndexConfig(indexSetService.findByIds(validIds));
    }
}
