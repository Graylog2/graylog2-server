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
import org.bson.types.ObjectId;
import org.graylog.events.search.MoreSearch;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.inputs.metrics.InputType;
import org.graylog2.inputs.metrics.TypedInputId;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.metrics.entity.cache.EntityCachedMetricDescriptor;
import org.graylog2.metrics.entity.cache.MetricsCacheConfiguration;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;
import static org.graylog2.plugin.Message.FIELD_STREAMS;

/**
 * Provides {@code associated_inputs} metrics for streams.
 * Returns the list of inputs (id + type) that sent messages to each stream in the last 24h.
 * <p>
 * The cache stores typed input IDs ({@link TypedInputId}). The type is established once at
 * {@link #compute} time by asking each registered {@link InputType} which IDs belong to its
 * collection — moving the cross-collection lookups off the hot per-poll path. On read,
 * {@link #computeForUser} filters by per-user Shiro permission checks keyed by the embedded
 * type, with no further DB lookups.
 * </p>
 */
public class StreamAssociatedInputsDescriptor
        implements EntityCachedMetricDescriptor<List<TypedInputId>, List<TypedInputId>> {

    public static final String FIELD_NAME = "associated_inputs";

    private final MoreSearch moreSearch;
    private final Duration cacheTtl;
    private final Set<InputType> inputTypes;

    @Inject
    public StreamAssociatedInputsDescriptor(MoreSearch moreSearch,
                                            @Named(MetricsCacheConfiguration.METRICS_CACHE_TTL_LONG) Duration cacheTtl,
                                            Set<InputType> inputTypes) {
        this.moreSearch = moreSearch;
        this.cacheTtl = cacheTtl;
        this.inputTypes = inputTypes;
    }

    @Override
    public TypeReference<List<TypedInputId>> cacheType() {
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
    public List<EntityMetric<List<TypedInputId>>> compute(Collection<String> entityIds) {
        final Map<String, Map<String, Long>> grouped = moreSearch.aggregateGroupedTerms(
                entityIds.stream()
                        .map(id -> FIELD_STREAMS + ":" + MoreSearch.luceneEscape(id))
                        .collect(Collectors.joining(" OR ")),
                RelativeRange.create(MetricsCacheConfiguration.RANGE_SECONDS_24H),
                FIELD_STREAMS, FIELD_GL2_SOURCE_INPUT,
                entityIds.size(), MetricsCacheConfiguration.MAX_TERMS_SIZE, entityIds);

        // Tag every ID we found with its type, in one bulk lookup per registered InputType.
        final Set<String> allIds = grouped.values().stream()
                .flatMap(m -> m.keySet().stream())
                .collect(Collectors.toSet());
        final Map<String, String> typeById = tagTypes(allIds);

        return entityIds.stream()
                .map(streamId ->
                        new EntityMetric<>(streamId, grouped.getOrDefault(streamId, Map.of()).keySet().stream()
                                .filter(typeById::containsKey)
                                .map(inputId -> new TypedInputId(inputId, typeById.get(inputId)))
                                .toList())
                ).toList();
    }

    @Override
    public List<TypedInputId> computeForUser(List<TypedInputId> cachedValue, SearchUser searchUser) {
        final Map<String, String> readPermissionByType = inputTypes.stream()
                .collect(Collectors.toMap(InputType::typeName, InputType::readPermission));

        return cachedValue.stream()
                .filter(ti -> {
                    final String perm = readPermissionByType.get(ti.type());
                    return perm != null && searchUser.isPermitted(perm, ti.id());
                })
                .toList();
    }

    private Map<String, String> tagTypes(Set<String> allIds) {
        final Set<String> remaining = allIds.stream()
                .filter(ObjectId::isValid)
                .collect(Collectors.toCollection(HashSet::new));
        if (remaining.isEmpty()) {
            return Map.of();
        }
        return inputTypes.stream()
                .flatMap(type -> {
                    final Set<String> members = type.filterMembers(remaining);
                    remaining.removeAll(members);
                    return members.stream().map(id -> Map.entry(id, type.typeName()));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
    }
}
