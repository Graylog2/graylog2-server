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
package org.graylog2.metrics.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.metrics.entity.cache.EntityCachedMetricDescriptor;
import org.graylog2.metrics.entity.cache.MetricsCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Generic service that orchestrates entity metrics: cache lookup, permission filtering,
 * and delegating computation to the appropriate {@link EntityMetricDescriptor}.
 * <p>
 * Each entity type (input, stream, etc.) gets its own instance, constructed with the set
 * of descriptors registered for that entity type via Guice multibinding.
 * </p>
 */
public class EntityMetricsService {
    private static final Logger LOG = LoggerFactory.getLogger(EntityMetricsService.class);

    private final String entityType;
    private final Map<String, EntityMetricDescriptor> descriptorsByField;
    private final MetricsCacheService cacheService;
    private final ObjectMapper objectMapper;

    public EntityMetricsService(String entityType,
                                Set<EntityMetricDescriptor> descriptors,
                                MetricsCacheService cacheService,
                                ObjectMapper objectMapper) {
        this.entityType = entityType;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.descriptorsByField = descriptors.stream()
                .collect(Collectors.toMap(EntityMetricDescriptor::fieldName, d -> d));
    }

    /**
     * Returns metrics for the given entity IDs.
     *
     * @param entityIds  the entity IDs to get metrics for
     * @param fields     the fields to return
     * @param searchUser the current user for search permissions
     * @return metric values per entity
     */
    public EntityMetricValues getMetrics(Collection<String> entityIds,
                                         Set<String> fields,
                                         SearchUser searchUser) {
        if (entityIds.isEmpty()) {
            return EntityMetricValues.builder().build();
        }
        validateFields(fields);

        final var builder = EntityMetricValues.builder();
        final Map<String, EntityCachedMetricDescriptor<?, ?>> cachedDescriptors = new HashMap<>();

        for (final String field : fields) {
            final EntityMetricDescriptor descriptor = descriptorsByField.get(field);
            if (descriptor instanceof EntityCachedMetricDescriptor<?, ?> cached) {
                cachedDescriptors.put(field, cached);
            } else if (descriptor instanceof EntityUncachedMetricDescriptor<?> uncached) {
                uncached.compute(entityIds, searchUser)
                        .forEach(metric -> builder.put(metric.entityId(), uncached.fieldName(), metric.value()));
            }
        }

        if (!cachedDescriptors.isEmpty()) {
            final Map<String, Duration> fieldTtls = cachedDescriptors.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().cacheTtl()));

            final var cacheResult = cacheService.checkCache(entityIds, entityType, fieldTtls, fieldTtls.keySet());
            mergeCachedFields(cacheResult.freshFields(), cachedDescriptors, searchUser, builder);

            for (final var entry : cachedDescriptors.entrySet()) {
                final Set<String> staleIds = cacheResult.staleEntityIdsForField(entry.getKey());
                if (!staleIds.isEmpty()) {
                    computeAndCache(entry.getValue(), staleIds, searchUser, builder);
                }
            }
        }

        return builder.build();
    }

    private void mergeCachedFields(Map<String, Map<String, Object>> freshFields,
                                   Map<String, EntityCachedMetricDescriptor<?, ?>> cachedDescriptors,
                                   SearchUser searchUser,
                                   EntityMetricValues.Builder builder) {
        // Entries that fail to deserialize are typically left over from a previous version with a
        // different cache schema. Treat them as stale and recompute below, which overwrites the
        // broken cache entries with fresh ones.
        final Map<String, Set<String>> incompatibleByField = new HashMap<>();
        freshFields.forEach((entityId, fields) ->
                fields.forEach((fieldName, value) -> {
                    final EntityCachedMetricDescriptor<?, ?> descriptor = cachedDescriptors.get(fieldName);
                    try {
                        builder.put(entityId, fieldName, applyFilter(descriptor, value, searchUser));
                    } catch (IncompatibleCachedValueException e) {
                        LOG.debug("Cached metric value for entity {} field {} is incompatible with the current cache schema ({}); recomputing.",
                                entityId, fieldName, descriptor.cacheType().getType(), e);
                        incompatibleByField.computeIfAbsent(fieldName, k -> new HashSet<>()).add(entityId);
                    }
                })
        );
        incompatibleByField.forEach((fieldName, ids) ->
                computeAndCache(cachedDescriptors.get(fieldName), ids, searchUser, builder));
    }

    private <C, R> void computeAndCache(EntityCachedMetricDescriptor<C, R> descriptor,
                                        Collection<String> entityIds,
                                        SearchUser searchUser,
                                        EntityMetricValues.Builder builder) {
        final List<EntityMetric<C>> computed = descriptor.compute(entityIds);
        final Map<String, Object> forCache = new HashMap<>();
        for (final var metric : computed) {
            forCache.put(metric.entityId(), metric.value());
            final R filteredValue = descriptor.computeForUser(metric.value(), searchUser);
            builder.put(metric.entityId(), descriptor.fieldName(), filteredValue);
        }
        cacheService.putFieldBatch(entityType, descriptor.fieldName(), forCache);
    }

    private <C, R> R applyFilter(EntityCachedMetricDescriptor<C, R> descriptor,
                                  Object cachedValue,
                                  SearchUser searchUser) {
        final C typedValue;
        try {
            typedValue = objectMapper.convertValue(cachedValue, descriptor.cacheType());
        } catch (IllegalArgumentException e) {
            throw new IncompatibleCachedValueException(e);
        }
        return descriptor.computeForUser(typedValue, searchUser);
    }

    private void validateFields(Set<String> fields) {
        fields.stream()
                .filter(field -> !descriptorsByField.containsKey(field))
                .findFirst()
                .ifPresent(field -> {
                    throw new BadRequestException(
                            f("Invalid field: %s. Valid fields: %s", field, descriptorsByField.keySet()));
                });
    }

    /**
     * Thrown when a cached metric value can't be deserialized into the descriptor's current
     * {@link EntityCachedMetricDescriptor#cacheType()}. Typically indicates a leftover entry from a
     * previous version whose cache schema differs.
     */
    private static class IncompatibleCachedValueException extends RuntimeException {
        IncompatibleCachedValueException(Throwable cause) {
            super(cause);
        }
    }
}
