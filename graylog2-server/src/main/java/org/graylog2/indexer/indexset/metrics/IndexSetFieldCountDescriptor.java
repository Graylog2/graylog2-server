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
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.metrics.entity.EntityMetric;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.metrics.entity.cache.MetricsCacheConfiguration.METRICS_CACHE_TTL_LONG;

/**
 * Cached {@code field_count} metric, the number of fields in the active write index of each index set, or no value
 * when that index or its field types are unknown.
 */
public class IndexSetFieldCountDescriptor extends IndexSetMetricDescriptor<Integer> {
    public static final String FIELD_NAME = "field_count";

    private final Indices indices;
    private final IndexFieldTypesService indexFieldTypesService;

    @Inject
    public IndexSetFieldCountDescriptor(Indices indices,
                                        IndexFieldTypesService indexFieldTypesService,
                                        IndexSetService indexSetService,
                                        IndexSetRegistry indexSetRegistry,
                                        @Named(METRICS_CACHE_TTL_LONG) Duration cacheTtl) {
        super(indexSetService, indexSetRegistry, cacheTtl);
        this.indices = indices;
        this.indexFieldTypesService = indexFieldTypesService;
    }

    @Override
    public String fieldName() {
        return FIELD_NAME;
    }

    @Override
    public TypeReference<Integer> cacheType() {
        return new TypeReference<>() {};
    }

    @Override
    public List<EntityMetric<Integer>> compute(Collection<String> entityIds) {
        final Set<IndexSet> indexSets = indexSets(entityIds);
        final Map<String, String> writeIndexByAlias = resolveWriteIndices(indexSets);
        final Map<String, Integer> fieldCounts = indexFieldTypesService.findByIndexNames(writeIndexByAlias.values()).stream()
                .collect(Collectors.toMap(IndexFieldTypesDTO::indexName, dto -> dto.fields().size()));

        return indexSets.stream()
                .map(indexSet -> {
                    final String writeIndex = writeIndexByAlias.get(indexSet.getWriteIndexAlias());
                    return new EntityMetric<>(indexSet.getConfig().id(), writeIndex == null ? null : fieldCounts.get(writeIndex));
                })
                .toList();
    }

    private Map<String, String> resolveWriteIndices(Set<IndexSet> indexSets) {
        final Set<String> writeAliases = indexSets.stream().map(IndexSet::getWriteIndexAlias).collect(Collectors.toSet());
        final Map<String, Set<String>> targetsByAlias = new HashMap<>();
        indices.getIndexNamesAndAliases(indexSets.stream().map(IndexSet::getIndexWildcard).toList())
                .forEach((index, aliases) -> aliases.stream()
                        .filter(writeAliases::contains)
                        .forEach(alias -> targetsByAlias.computeIfAbsent(alias, key -> new HashSet<>()).add(index)));
        // an alias pointing at several indices stays unresolved, as aliasTarget() refuses it as well
        // the write alias matches its own wildcard, which exposes every target even outside it
        return targetsByAlias.entrySet().stream()
                .filter(entry -> entry.getValue().size() == 1)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().iterator().next()));
    }
}
