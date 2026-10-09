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
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.metrics.entity.cache.CacheResult;
import org.graylog2.metrics.entity.cache.EntityCachedMetricDescriptor;
import org.graylog2.metrics.entity.cache.MetricsCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.metrics.entity.EntityMetricsModule.ENTITY_TYPE_INDEX_SETS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexSetMetricsRefreshPeriodicalTest {
    private static final Set<String> FIELDS = Set.of("health", "size");

    @Mock
    private MetricsCacheService cacheService;
    @Mock
    private IndexSetService indexSetService;

    private final FakeDescriptor health = new FakeDescriptor("health", Duration.ofMinutes(1));
    private final FakeDescriptor size = new FakeDescriptor("size", Duration.ofMinutes(5));
    private IndexSetMetricsRefreshPeriodical periodical;

    @BeforeEach
    void setUp() {
        periodical = new IndexSetMetricsRefreshPeriodical(Set.of(health, size), cacheService, indexSetService, Duration.ofMinutes(1));
    }

    @Test
    void doRun_recomputesOnlyValuesThatExpireBeforeTheNextRun() {
        final List<IndexSetConfig> indexSets = List.of(indexSetConfig("a"), indexSetConfig("b"));
        when(indexSetService.findAll()).thenReturn(indexSets);
        final CacheResult.Builder cache = CacheResult.builder(List.of("a", "b"), FIELDS);
        cache.markFound("a");
        cache.addStale("a", "health");
        cache.addFresh("a", "size", 1L);
        cache.markFound("b");
        cache.addFresh("b", "health", "Green");
        cache.addFresh("b", "size", 2L);
        when(cacheService.checkCache(List.of("a", "b"), ENTITY_TYPE_INDEX_SETS,
                Map.of("health", Duration.ZERO, "size", Duration.ofMinutes(4)), FIELDS)).thenReturn(cache.build());

        periodical.doRun();

        assertThat(health.computeCalls).containsExactly(Set.of("a"));
        assertThat(size.computeCalls).isEmpty();
        verify(cacheService).putFieldBatch(ENTITY_TYPE_INDEX_SETS, "health", Map.of("a", 42L));
        verify(cacheService, times(1)).putFieldBatch(any(), any(), any());
    }

    @Test
    void runsOnTheLeaderOncePerShortTtl() {
        assertThat(periodical.leaderOnly()).isTrue();
        assertThat(periodical.getInitialDelaySeconds()).isGreaterThanOrEqualTo(60);
        assertThat(periodical.getPeriodSeconds()).isEqualTo(60);
    }

    private static IndexSetConfig indexSetConfig(String id) {
        final IndexSetConfig config = mock(IndexSetConfig.class);
        when(config.id()).thenReturn(id);
        return config;
    }

    private static final class FakeDescriptor implements EntityCachedMetricDescriptor<Long, Long> {
        private final String fieldName;
        private final Duration cacheTtl;
        private final List<Collection<String>> computeCalls = new ArrayList<>();

        FakeDescriptor(String fieldName, Duration cacheTtl) {
            this.fieldName = fieldName;
            this.cacheTtl = cacheTtl;
        }

        @Override
        public String fieldName() {
            return fieldName;
        }

        @Override
        public Duration cacheTtl() {
            return cacheTtl;
        }

        @Override
        public TypeReference<Long> cacheType() {
            return new TypeReference<>() {};
        }

        @Override
        public List<EntityMetric<Long>> compute(Collection<String> entityIds) {
            computeCalls.add(entityIds);
            return entityIds.stream().map(id -> new EntityMetric<>(id, 42L)).toList();
        }

        @Override
        public Long computeForUser(Long cachedValue, SearchUser searchUser) {
            return cachedValue;
        }
    }
}
