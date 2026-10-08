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

import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.metrics.entity.EntityMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexSetMetricsRefreshPeriodicalTest {
    @Mock
    private EntityMetricsService metricsService;
    @Mock
    private IndexSetService indexSetService;

    private IndexSetMetricsRefreshPeriodical periodical;

    @BeforeEach
    void setUp() {
        periodical = new IndexSetMetricsRefreshPeriodical(metricsService, indexSetService, Duration.ofMinutes(1));
    }

    @Test
    void doRun_refreshesEveryIndexSet() {
        final List<IndexSetConfig> indexSets = List.of(indexSetConfig("a"), indexSetConfig("b"));
        when(indexSetService.findAll()).thenReturn(indexSets);

        periodical.doRun();

        verify(metricsService).refresh(List.of("a", "b"));
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
}
