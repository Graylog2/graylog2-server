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

import com.fasterxml.jackson.core.type.TypeReference;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.metrics.entity.cache.EntityCachedMetricDescriptor;
import org.graylog2.metrics.entity.cache.MetricsCacheService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        final var descriptor = new TestCachedDescriptor(
                "message_count",
                Duration.ofMinutes(5),
                // compute returns per-stream breakdown
                Map.of(
                        "entity-1", Map.of("stream-a", 100L, "stream-b", 200L),
                        "entity-2", Map.of("stream-a", 50L, "stream-c", 150L)
                )
        );

        when(searchUser.canReadStream("stream-a")).thenReturn(true);
        when(searchUser.canReadStream("stream-b")).thenReturn(false);
        when(searchUser.canReadStream("stream-c")).thenReturn(false);

        final var service = createService(descriptor);
        final var result = service.getMetrics(
                List.of("entity-1", "entity-2"),
                Set.of("message_count"),
                searchUser);

        // entity-1: only stream-a permitted → filtered map
        assertThat(result.toMap().get("entity-1")).containsEntry("message_count", Map.of("stream-a", 100L));
        // entity-2: only stream-a permitted → filtered map
        assertThat(result.toMap().get("entity-2")).containsEntry("message_count", Map.of("stream-a", 50L));
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

        assertThat(firstResult.toMap().get("entity-1")).containsEntry("message_count", Map.of("stream-a", 100L, "stream-b", 200L));

        // Second call with different user — reads from cache, filters differently
        final SearchUser restrictedUser = mock(SearchUser.class);
        when(restrictedUser.canReadStream("stream-a")).thenReturn(true);
        when(restrictedUser.canReadStream("stream-b")).thenReturn(false);

        final var secondResult = service.getMetrics(
                List.of("entity-1"), Set.of("message_count"), restrictedUser);

        assertThat(secondResult.toMap().get("entity-1")).containsEntry("message_count", Map.of("stream-a", 100L));
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
                Set.of(cachedDescriptor, uncachedDescriptor), cacheService, new ObjectMapperProvider().get());
        final var result = service.getMetrics(
                List.of("entity-1"), Set.of("message_count", "pipeline_count"), searchUser);

        assertThat(result.toMap().get("entity-1"))
                .containsEntry("message_count", Map.of("stream-a", 500L))
                .containsEntry("pipeline_count", 2);
    }

    @Test
    void cacheHit_withIncompatibleCachedValue_triggersRecompute() {
        cacheService.putFieldBatch(ENTITY_TYPE, "message_count", Map.of("entity-1", "stale-shape"));

        when(searchUser.canReadStream("stream-a")).thenReturn(true);
        final var descriptor = new TestCachedDescriptor(
                "message_count", Duration.ofMinutes(5),
                Map.of("entity-1", Map.of("stream-a", 42L)));

        final var result = createService(descriptor).getMetrics(
                List.of("entity-1"), Set.of("message_count"), searchUser);

        assertThat(result.toMap().get("entity-1")).containsEntry("message_count", Map.of("stream-a", 42L));
    }

    private EntityMetricsService createService(EntityMetricDescriptor descriptor) {
        return new EntityMetricsService(ENTITY_TYPE, Set.of(descriptor), cacheService, new ObjectMapperProvider().get());
    }

    /**
     * Test cached descriptor that stores per-stream breakdowns and filters by stream permissions.
     */
    private static class TestCachedDescriptor implements EntityCachedMetricDescriptor<Map<String, Long>, Map<String, Long>> {
        private final String fieldName;
        private final Duration cacheTtl;
        private final List<EntityMetric<Map<String, Long>>> computeResult;

        TestCachedDescriptor(String fieldName, Duration cacheTtl, Map<String, Map<String, Long>> computeResult) {
            this.fieldName = fieldName;
            this.cacheTtl = cacheTtl;
            this.computeResult = computeResult.entrySet().stream()
                    .map(e -> new EntityMetric<>(e.getKey(), e.getValue()))
                    .toList();
        }

        @Override
        public TypeReference<Map<String, Long>> cacheType() { return new TypeReference<>() {}; }

        @Override
        public String fieldName() { return fieldName; }

        @Override
        public Duration cacheTtl() { return cacheTtl; }

        @Override
        public List<EntityMetric<Map<String, Long>>> compute(Collection<String> entityIds) {
            return computeResult;
        }

        @Override
        public Map<String, Long> computeForUser(Map<String, Long> countsByStream, SearchUser searchUser) {
            return countsByStream.entrySet().stream()
                    .filter(e -> searchUser.canReadStream(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    /**
     * Test uncached descriptor that filters pipelines by user permissions.
     */
    private static class TestUncachedDescriptor implements EntityUncachedMetricDescriptor<Integer> {
        private final String fieldName;

        TestUncachedDescriptor(String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public String fieldName() { return fieldName; }

        @Override
        public List<EntityMetric<Integer>> compute(Collection<String> entityIds, SearchUser searchUser) {
            final int count = (int) Stream.of("pipeline-1", "pipeline-2")
                    .filter(id -> searchUser.isPermitted("pipelines:read", id))
                    .count();
            return entityIds.stream()
                    .map(id -> new EntityMetric<>(id, count))
                    .toList();
        }
    }
}
