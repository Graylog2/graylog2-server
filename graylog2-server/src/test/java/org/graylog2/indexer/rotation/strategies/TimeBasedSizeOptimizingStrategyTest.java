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

import org.graylog.events.JobSchedulerTestClock;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Period;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategy.MAX_INDEX_SIZE;
import static org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategy.MIN_INDEX_SIZE;
import static org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategy.ROTATION_PERIOD;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeBasedSizeOptimizingStrategyTest {

    private TimeBasedSizeOptimizingStrategy timeBasedSizeOptimizingStrategy;

    @Mock
    private Indices indices;

    @Mock
    private NodeId nodeId;

    @Mock
    private AuditEventSender auditEventSender;

    @Mock
    private IndexSet indexSet;

    private TimeBasedSizeOptimizingStrategyConfig timeBasedSizeOptimizingStrategyConfig;
    private JobSchedulerTestClock clock;

    @BeforeEach
    void setUp() {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        clock = new JobSchedulerTestClock(now);
        timeBasedSizeOptimizingStrategy = new TimeBasedSizeOptimizingStrategy(indices, nodeId, auditEventSender, new ElasticsearchConfiguration(), clock);

        timeBasedSizeOptimizingStrategyConfig = TimeBasedSizeOptimizingStrategyConfig.builder().build();
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        when(indexSetConfig.rotationStrategy()).thenReturn(timeBasedSizeOptimizingStrategyConfig);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2020-12-01T09:00:00Z", "now"})
    void shouldRotateWhenTooBig(String startDate) {
        setClockTo(startDate);

        final DateTime creationDate = clock.nowUTC().minus(Duration.standardMinutes(10));
        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(MAX_INDEX_SIZE.toBytes() + 10));

        final TimeBasedSizeOptimizingStrategy.Result result = timeBasedSizeOptimizingStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
        assertThat(result.getDescription()).contains("exceeds MAX_INDEX_SIZE");
    }

    private void setClockTo(String startDate) {
        if (startDate.equals("now")) {
            clock.setTime(DateTime.now(DateTimeZone.UTC));
        } else {
            clock.setTime(DateTime.parse(startDate));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"2020-12-01T09:00:00Z", "now"})
    void shouldRotateWhenRightSizedAndOverRotationPeriod(String startDate) {
        setClockTo(startDate);

        final DateTime creationDate = clock.nowUTC().minus(ROTATION_PERIOD);
        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(MIN_INDEX_SIZE.toBytes() + 10));

        final TimeBasedSizeOptimizingStrategy.Result result = timeBasedSizeOptimizingStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
        assertThat(result.getDescription()).contains("Index is old enough");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2020-12-01T09:00:00Z", "now"})
    void shouldRotateWhenBeyondLeeWay(String startDate) {
        setClockTo(startDate);

        final Period leeWay = timeBasedSizeOptimizingStrategyConfig.indexLifetimeHard().minus(timeBasedSizeOptimizingStrategyConfig.indexLifetimeSoft());
        final Days leewayDays = Days.days(leeWay.getDays());
        final DateTime creationDate = clock.nowUTC().minus(ROTATION_PERIOD.plus(leewayDays)).minus(Duration.millis(10)); // avoid race between the time of now here and in code

        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(5L));

        final TimeBasedSizeOptimizingStrategy.Result result = timeBasedSizeOptimizingStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
        assertThat(result.getDescription()).contains("exceeds optimization leeway");
    }
}
