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

import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.metrics.cache.EntityCachedMetricsDescriptor;
import org.graylog2.metrics.cache.EntityMetricsDescriptor;
import org.graylog2.metrics.cache.EntityUncachedMetricsDescriptor;
import org.graylog2.metrics.cache.MetricsCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class EntityMetricsServiceTest {

    private static final String ENTITY_TYPE = "test_entities";

    private MetricsCacheService cacheService;
    private SearchUser searchUser;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        cacheService = new MetricsCacheService(mongoCollections, Duration.ofHours(24), Clock.systemUTC());
        searchUser = mock(SearchUser.class);
    }

    @Test
    void cacheMiss_appliesPermissionFilter() {
        // Descriptor returns per-stream breakdown, filter sums permitted streams only
        final var descriptor = new TestCachedDescriptor(
                "message_count",
                Duration.ofMinutes(5),
                // computeField returns per-stream breakdown
                Map.of(
                        "entity-1", Map.of("stream-a", 100L, "stream-b", 200L),
                        "entity-2", Map.of("stream-a", 50L, "stream-c", 150L)
                )
        );

        // User can read stream-a but not stream-b or stream-c
        when(searchUser.canReadStream("stream-a")).thenReturn(true);
        when(searchUser.canReadStream("stream-b")).thenReturn(false);
        when(searchUser.canReadStream("stream-c")).thenReturn(false);

        final var service = createService(descriptor);
        final var result = service.getMetrics(
                List.of("entity-1", "entity-2"),
                Set.of("message_count"),
                searchUser);

        // entity-1: only stream-a counts → 100
        assertThat(result.toMap().get("entity-1")).containsEntry("message_count", 100L);
        // entity-2: only stream-a counts → 50
        assertThat(result.toMap().get("entity-2")).containsEntry("message_count", 50L);
    }

    @Test
    void cacheHit_appliesPermissionFilter() {
        final var descriptor = new TestCachedDescriptor(
                "message_count",
                Duration.ofMinutes(5),
                Map.of("entity-1", Map.of("stream-a", 100L, "stream-b", 200L))
        );

        // First call — populates cache
        when(searchUser.canReadStream("stream-a")).thenReturn(true);
        when(searchUser.canReadStream("stream-b")).thenReturn(true);

        final var service = createService(descriptor);
        final var firstResult = service.getMetrics(
                List.of("entity-1"), Set.of("message_count"), searchUser);

        assertThat(firstResult.toMap().get("entity-1")).containsEntry("message_count", 300L);

        // Second call with different user — reads from cache, filters differently
        final SearchUser restrictedUser = mock(SearchUser.class);
        when(restrictedUser.canReadStream("stream-a")).thenReturn(true);
        when(restrictedUser.canReadStream("stream-b")).thenReturn(false);

        final var secondResult = service.getMetrics(
                List.of("entity-1"), Set.of("message_count"), restrictedUser);

        assertThat(secondResult.toMap().get("entity-1")).containsEntry("message_count", 100L);
    }

    @Test
    void uncachedDescriptor_receivesSearchUser() {
        final var descriptor = new TestUncachedDescriptor("pipeline_count");

        when(searchUser.isPermitted("pipelines:read", "pipeline-1")).thenReturn(true);
        when(searchUser.isPermitted("pipelines:read", "pipeline-2")).thenReturn(false);

        final var service = createService(descriptor);
        final var result = service.getMetrics(
                List.of("entity-1"), Set.of("pipeline_count"), searchUser);

        // The uncached descriptor filters by user — only counts permitted pipelines
        assertThat(result.toMap().get("entity-1")).containsEntry("pipeline_count", 1);
    }

    @Test
    void mixedDescriptors_cachedAndUncached() {
        final var cachedDescriptor = new TestCachedDescriptor(
                "message_count", Duration.ofMinutes(5),
                Map.of("entity-1", Map.of("stream-a", 500L))
        );
        final var uncachedDescriptor = new TestUncachedDescriptor("pipeline_count");

        when(searchUser.canReadStream("stream-a")).thenReturn(true);
        when(searchUser.isPermitted("pipelines:read", "pipeline-1")).thenReturn(true);
        when(searchUser.isPermitted("pipelines:read", "pipeline-2")).thenReturn(true);

        final var service = new EntityMetricsService(ENTITY_TYPE,
                Set.of(cachedDescriptor, uncachedDescriptor), cacheService);
        final var result = service.getMetrics(
                List.of("entity-1"), Set.of("message_count", "pipeline_count"), searchUser);

        assertThat(result.toMap().get("entity-1"))
                .containsEntry("message_count", 500L)
                .containsEntry("pipeline_count", 2);
    }

    private EntityMetricsService createService(EntityMetricsDescriptor descriptor) {
        return new EntityMetricsService(ENTITY_TYPE, Set.of(descriptor), cacheService);
    }

    /**
     * Test cached descriptor that stores per-stream breakdowns and filters by stream permissions.
     */
    private static class TestCachedDescriptor implements EntityCachedMetricsDescriptor {
        private final String fieldName;
        private final Duration cacheTtl;
        private final Map<String, Object> computeResult;

        TestCachedDescriptor(String fieldName, Duration cacheTtl, Map<String, ?> computeResult) {
            this.fieldName = fieldName;
            this.cacheTtl = cacheTtl;
            this.computeResult = new HashMap<>();
            computeResult.forEach((k, v) -> this.computeResult.put(k, v));
        }

        @Override
        public String entityType() { return ENTITY_TYPE; }

        @Override
        public String fieldName() { return fieldName; }

        @Override
        public Duration cacheTtl() { return cacheTtl; }

        @Override
        public Map<String, Object> computeField(Collection<String> entityIds) {
            return computeResult;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object applyPermissionFilter(Object cachedValue, SearchUser searchUser) {
            final Map<String, Long> countsByStream = (Map<String, Long>) cachedValue;
            return countsByStream.entrySet().stream()
                    .filter(e -> searchUser.canReadStream(e.getKey()))
                    .mapToLong(Map.Entry::getValue)
                    .sum();
        }
    }

    /**
     * Test uncached descriptor that filters pipelines by user permissions.
     */
    private static class TestUncachedDescriptor implements EntityUncachedMetricsDescriptor {
        private final String fieldName;

        TestUncachedDescriptor(String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public String entityType() { return ENTITY_TYPE; }

        @Override
        public String fieldName() { return fieldName; }

        @Override
        public Map<String, Object> computeField(Collection<String> entityIds, SearchUser searchUser) {
            // Simulates counting pipelines the user can read
            final int count = List.of("pipeline-1", "pipeline-2").stream()
                    .filter(id -> searchUser.isPermitted("pipelines:read", id))
                    .toList()
                    .size();
            final Map<String, Object> result = new HashMap<>();
            for (final String entityId : entityIds) {
                result.put(entityId, count);
            }
            return result;
        }
    }
}
