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

import com.github.joschi.jadconfig.util.Size;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.retention.strategies.ClosingRetentionStrategyConfig;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
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

    @Mock
    private IndexSetConfig indexSetConfig;

    private TimeBasedSizeOptimizingStrategyConfig timeBasedSizeOptimizingStrategyConfig;
    private JobSchedulerTestClock clock;

    private ElasticsearchConfiguration elasticsearchConfiguration;

    @BeforeEach
    void setUp() {
        elasticsearchConfiguration = spy(new ElasticsearchConfiguration());
        DateTime now = DateTime.now(DateTimeZone.UTC);
        clock = new JobSchedulerTestClock(now);
        timeBasedSizeOptimizingStrategy = new TimeBasedSizeOptimizingStrategy(indices, nodeId, auditEventSender, elasticsearchConfiguration, clock);

        timeBasedSizeOptimizingStrategyConfig = TimeBasedSizeOptimizingStrategyConfig.builder().build();
        when(indexSetConfig.shards()).thenReturn(1);

        when(indexSetConfig.rotationStrategy()).thenReturn(timeBasedSizeOptimizingStrategyConfig);
        lenient().when(indexSetConfig.retentionStrategy()).thenReturn(ClosingRetentionStrategyConfig.createDefault());
        when(indexSet.getConfig()).thenReturn(indexSetConfig);

        when(indices.numberOfMessages(anyString())).thenReturn(10L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2020-12-01T09:00:00Z", "now"})
    void shouldRotateWhenTooBig(String startDate) {
        setClockTo(startDate);

        final DateTime creationDate = clock.nowUTC().minus(Duration.standardMinutes(10));
        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        final Size timeSizeOptimizingRotationMaxSize = elasticsearchConfiguration.getTimeSizeOptimizingRotationMaxShardSize();
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(timeSizeOptimizingRotationMaxSize.toBytes() + 10));

        final TimeBasedSizeOptimizingStrategy.Result result = timeBasedSizeOptimizingStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
        assertThat(result.getDescription()).contains("exceeds maximum size");
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

        final DateTime creationDate = clock.nowUTC().minus(elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod());
        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        final Size timeSizeOptimizingRotationMinSize = elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize();
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(timeSizeOptimizingRotationMinSize.toBytes() + 10));

        final TimeBasedSizeOptimizingStrategy.Result result = timeBasedSizeOptimizingStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
        assertThat(result.getDescription()).contains("has passed rotation period");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2020-12-01T09:00:00Z", "now"})
    void shouldRotateWhenBeyondLeeWay(String startDate) {
        setClockTo(startDate);

        final Period leeWay = timeBasedSizeOptimizingStrategyConfig.indexLifetimeMax().minus(timeBasedSizeOptimizingStrategyConfig.indexLifetimeMin());
        final Days leewayDays = Days.days(leeWay.getDays());
        final DateTime creationDate = clock.nowUTC().minus(
                elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod().plus(leewayDays))
                .minus(Duration.millis(10)); // avoid race between the time of now here and in code

        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(5L));

        final TimeBasedSizeOptimizingStrategy.Result result = timeBasedSizeOptimizingStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
        assertThat(result.getDescription()).contains("exceeds optimization leeway");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2020-12-01T09:00:00Z", "now"})
    void shouldRotateWhenRightSizedAndOverCustomRotationPeriod(String startDate) {
        setClockTo(startDate);

        final Period customRotationPeriod = Period.hours(12);
        when(elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod()).thenReturn(customRotationPeriod);
        timeBasedSizeOptimizingStrategy = new TimeBasedSizeOptimizingStrategy(indices, nodeId, auditEventSender, elasticsearchConfiguration, clock);

        final DateTime creationDate = clock.nowUTC().minus(customRotationPeriod);
        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        final Size timeSizeOptimizingRotationMinSize = elasticsearchConfiguration.getTimeSizeOptimizingRotationMinShardSize();
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(timeSizeOptimizingRotationMinSize.toBytes() + 10));

        final TimeBasedSizeOptimizingStrategy.Result result = timeBasedSizeOptimizingStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
        assertThat(result.getDescription()).contains("has passed rotation period");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2020-12-01T09:00:00Z", "now"})
    void shouldIgnoreLeeWayWhenNoopRetention(String startDate) {
        setClockTo(startDate);

        final Period leeWay = timeBasedSizeOptimizingStrategyConfig.indexLifetimeMax().minus(timeBasedSizeOptimizingStrategyConfig.indexLifetimeMin());
        final Days leewayDays = Days.days(leeWay.getDays());
        final DateTime creationDate = clock.nowUTC().minus(
                        elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod().plus(leewayDays))
                .minus(Duration.millis(10)); // avoid race between the time of now here and in code

        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(5L));

        when(indexSetConfig.retentionStrategy()).thenReturn(NoopRetentionStrategyConfig.createDefault());

        final TimeBasedSizeOptimizingStrategy.Result result = timeBasedSizeOptimizingStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(false);
    }
}
