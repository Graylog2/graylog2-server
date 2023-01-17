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
package org.graylog2.indexer.rotation.strategies;

import org.graylog2.audit.AuditEventSender;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Period;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.rotation.strategies.SmartRotationStrategy.MAX_INDEX_SIZE;
import static org.graylog2.indexer.rotation.strategies.SmartRotationStrategy.MIN_INDEX_SIZE;
import static org.graylog2.indexer.rotation.strategies.SmartRotationStrategy.ROTATION_PERIOD;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartRotationStrategyTest {

    private SmartRotationStrategy smartRotationStrategy;

    @Mock
    private Indices indices;

    @Mock
    private NodeId nodeId;

    @Mock
    private AuditEventSender auditEventSender;

    @Mock
    private IndexSet indexSet;

    private SmartRotationStrategyConfig smartRotationStrategyConfig;
    private DateTime now;

    @BeforeEach
    void setUp() {
        smartRotationStrategy = new SmartRotationStrategy(indices, nodeId, auditEventSender, new ElasticsearchConfiguration());

        smartRotationStrategyConfig = SmartRotationStrategyConfig.builder().build();
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        when(indexSetConfig.rotationStrategy()).thenReturn(smartRotationStrategyConfig);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        now = DateTime.now(DateTimeZone.UTC);
    }

    @Test
    void shouldRotateWhenTooBig() {
        final DateTime creationDate = now.minus(Duration.standardMinutes(10));
        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(MAX_INDEX_SIZE.toBytes() + 10));

        final SmartRotationStrategy.Result result = smartRotationStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
        assertThat(result.getDescription()).contains("exceeds MAX_INDEX_SIZE");
    }

    @Test
    void shouldRotateWhenRightSizedAndOverRotationPeriod() {
        // TODO only works with multiple of days
        final DateTime creationDate = now.minus(Duration.standardDays(ROTATION_PERIOD.getDays()));
        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(MIN_INDEX_SIZE.toBytes() + 10));

        final SmartRotationStrategy.Result result = smartRotationStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
        assertThat(result.getDescription()).contains("Index is old enough");
    }

    @Test
    void shouldRotateWhenBeyondLeeWay() {
        final Period leeWay = smartRotationStrategyConfig.indexLifetimeHard().minus(smartRotationStrategyConfig.indexLifetimeSoft());
        // TODO only works with multiple of days
        final java.time.Duration leeWayDuration = java.time.Duration.ofDays(leeWay.getDays() + ROTATION_PERIOD.getDays());
        final DateTime creationDate = now.minus(leeWayDuration.toMillis());

        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(5L));

        final SmartRotationStrategy.Result result = smartRotationStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
        assertThat(result.getDescription()).contains("exceeds optimization leeway");
    }
}
