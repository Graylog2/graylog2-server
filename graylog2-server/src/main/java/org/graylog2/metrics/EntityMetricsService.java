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
package org.graylog2.metrics;

import jakarta.ws.rs.BadRequestException;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.metrics.cache.EntityCachedMetricsDescriptor;
import org.graylog2.metrics.cache.EntityMetricsDescriptor;
import org.graylog2.metrics.cache.MetricsCacheService;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Generic service that orchestrates entity metrics: cache lookup, permission filtering,
 * and delegating computation to the appropriate {@link EntityMetricsDescriptor}.
 * <p>
 * Each entity type (input, stream, etc.) gets its own instance, constructed with the set
 * of descriptors registered for that entity type via Guice multibinding.
 * </p>
 */
public class EntityMetricsService {

    private final String entityType;
    private final Map<String, EntityMetricsDescriptor> descriptorsByField;
    private final MetricsCacheService cacheService;

    public EntityMetricsService(String entityType,
                                Set<EntityMetricsDescriptor> descriptors,
                                MetricsCacheService cacheService) {
        this.entityType = entityType;
        this.cacheService = cacheService;
        this.descriptorsByField = descriptors.stream()
                .collect(Collectors.toMap(EntityMetricsDescriptor::fieldName, d -> d));
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
        validateFields(fields);

        final var builder = EntityMetricValues.builder();
        final Map<String, Duration> cachedFieldTtls = new HashMap<>();

        for (final String field : fields) {
            final EntityMetricsDescriptor descriptor = descriptorsByField.get(field);
            if (descriptor instanceof EntityCachedMetricsDescriptor cached) {
                cachedFieldTtls.put(field, cached.cacheTtl());
            } else {
                computeFresh(descriptor, entityIds, searchUser, builder);
            }
        }

        final var cacheResult = cachedFieldTtls.isEmpty()
                ? null
                : cacheService.checkCache(entityIds, entityType, cachedFieldTtls, cachedFieldTtls.keySet());

        if (cacheResult != null) {
            mergeCachedFields(cacheResult.freshFields(), searchUser, builder);

            for (final String field : cachedFieldTtls.keySet()) {
                final Set<String> staleIds = cacheResult.staleEntityIdsForField(field);
                if (!staleIds.isEmpty()) {
                    computeAndCache(descriptorsByField.get(field), staleIds, searchUser, builder);
                }
            }
        }

        return builder.build();
    }

    private void mergeCachedFields(Map<String, Map<String, Object>> freshFields,
                                   SearchUser searchUser,
                                   EntityMetricValues.Builder builder) {
        for (final var entityEntry : freshFields.entrySet()) {
            final String entityId = entityEntry.getKey();
            for (final var fieldEntry : entityEntry.getValue().entrySet()) {
                final String field = fieldEntry.getKey();
                final Object rawValue = fieldEntry.getValue();
                final EntityMetricsDescriptor descriptor = descriptorsByField.get(field);

                final Object filteredValue;
                if (descriptor instanceof EntityCachedMetricsDescriptor cached) {
                    filteredValue = cached.filterValue(rawValue, searchUser);
                } else {
                    filteredValue = rawValue;
                }
                builder.put(entityId, field, filteredValue);
            }
        }
    }

    private void computeAndCache(EntityMetricsDescriptor descriptor,
                                 Collection<String> entityIds,
                                 SearchUser searchUser,
                                 EntityMetricValues.Builder builder) {
        final Map<String, Object> computed = descriptor.computeField(entityIds, searchUser);
        cacheService.putFieldBatch(entityType, descriptor.fieldName(), computed);
        for (final var entry : computed.entrySet()) {
            builder.put(entry.getKey(), descriptor.fieldName(), entry.getValue());
        }
    }

    private void computeFresh(EntityMetricsDescriptor descriptor,
                              Collection<String> entityIds,
                              SearchUser searchUser,
                              EntityMetricValues.Builder builder) {
        final Map<String, Object> computed = descriptor.computeField(entityIds, searchUser);
        for (final var entry : computed.entrySet()) {
            builder.put(entry.getKey(), descriptor.fieldName(), entry.getValue());
        }
    }

    private void validateFields(Set<String> fields) {
        for (final String field : fields) {
            if (!descriptorsByField.containsKey(field)) {
                throw new BadRequestException(
                        f("Invalid field: %s. Valid fields: %s", field, descriptorsByField.keySet()));
            }
        }
    }
}
