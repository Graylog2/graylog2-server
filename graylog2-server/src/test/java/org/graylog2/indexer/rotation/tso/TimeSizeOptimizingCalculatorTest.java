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
package org.graylog2.indexer.rotation.tso;

import com.github.joschi.jadconfig.util.Size;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.common.IndexRotator;
import org.graylog2.system.stats.elasticsearch.NodeOSInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeSizeOptimizingCalculatorTest {

    private static final IndexLifetimeConfig DEFAULT_LIFETIME = IndexLifetimeConfig.builder()
            .indexLifetimeMax(IndexLifetimeConfig.DEFAULT_LIFETIME_MAX)
            .indexLifetimeMin(IndexLifetimeConfig.DEFAULT_LIFETIME_MIN)
            .build();
    private static final Size SHARD_SIZE_MIN_MAX = Size.gigabytes(20);
    private static final String INDEX_0 = "index_0";
    private static final Period ROTATION_PERIOD = Period.days(1);
    @Mock
    private Indices indices;
    @Mock
    private IndexSetConfig indexSetConfig;
    @Mock
    ClusterAdapter clusterAdapter;

    private ElasticsearchConfiguration elasticsearchConfiguration;
    private JobSchedulerTestClock clock;

    private TimeSizeOptimizingCalculator underTest;

    @BeforeEach
    void setUp() {
        elasticsearchConfiguration = new ElasticsearchConfiguration();
        elasticsearchConfiguration.setTimeSizeOptimizingRotationMaxShardSize(SHARD_SIZE_MIN_MAX);
        elasticsearchConfiguration.setTimeSizeOptimizingRotationMinShardSize(SHARD_SIZE_MIN_MAX);
        DateTime now = DateTime.now(DateTimeZone.UTC);
        clock = new JobSchedulerTestClock(now);
        underTest = new TimeSizeOptimizingCalculator(
                indices,
                clock,
                elasticsearchConfiguration,
                clusterAdapter
        );

        lenient().when(indexSetConfig.shards()).thenReturn(1);
        when(indices.numberOfMessages(anyString())).thenReturn(10L);
    }

    @Test
    void skipRotationIfNoMessages() {
        when(indices.indexCreationDate(INDEX_0)).thenReturn(Optional.of(clock.nowUTC()));
        when(indices.getStoreSizeInBytes(INDEX_0)).thenReturn(Optional.of(SHARD_SIZE_MIN_MAX.toBytes()));
        when(indices.numberOfMessages(anyString())).thenReturn(0L);

        final IndexRotator.Result result = underTest.calculate(INDEX_0, DEFAULT_LIFETIME, indexSetConfig);

        assertThat(result.shouldRotate()).isFalse();
        assertThat(result.getDescription()).contains("Index is empty");
    }

    @Test
    void shouldNotRotateUsingDynamicShardSizingWithFallback() {
        enableDynamicShardSizing();
        when(indices.indexCreationDate(INDEX_0)).thenReturn(Optional.of(clock.nowUTC()));
        when(clusterAdapter.nodesHostInfo()).thenReturn(Map.of());
        when(indices.getStoreSizeInBytes(INDEX_0)).thenReturn(Optional.of(1L));

        final IndexRotator.Result result = underTest.calculate(INDEX_0, DEFAULT_LIFETIME, indexSetConfig);

        assertThat(result.shouldRotate()).isFalse();
        assertThat(result.getDescription()).contains("No reason to rotate found");
    }

    @Test
    void shouldRotateUsingDynamicShardSizingWithFallback() {
        enableDynamicShardSizing();
        when(indices.indexCreationDate(INDEX_0)).thenReturn(Optional.of(clock.nowUTC()));
        when(clusterAdapter.nodesHostInfo()).thenReturn(Map.of());
        when(indices.getStoreSizeInBytes(INDEX_0)).thenReturn(Optional.of(Size.gigabytes(21).toBytes()));

        final IndexRotator.Result result = underTest.calculate(INDEX_0, DEFAULT_LIFETIME, indexSetConfig);

        assertThat(result.shouldRotate()).isTrue();
        assertThat(result.getDescription()).contains("Fallback to shard size <20.0 GiB>");
    }

    @Test
    void shouldRotateUsingDynamicShardSizingWithMinValue() {
        enableDynamicShardSizing();
        when(indices.indexCreationDate(INDEX_0)).thenReturn(Optional.of(clock.nowUTC()));
        when(clusterAdapter.nodesHostInfo()).thenReturn(Map.of("n1", node(1), "n2", node(4)));
        when(indices.getStoreSizeInBytes(INDEX_0)).thenReturn(Optional.of(Size.gigabytes(6).toBytes()));

        final IndexRotator.Result result = underTest.calculate(INDEX_0, DEFAULT_LIFETIME, indexSetConfig);

        assertThat(result.shouldRotate()).isTrue();
        assertThat(result.getDescription()).contains("Dynamic shard size <1.0 GiB * 0.6 ≈ 614.4 MiB> is less than minimum. Using minimum shard size <5.0 GiB>");
    }

    @Test
    void shouldRotateUsingDynamicShardSizing() {
        int dataSizeGigabytes = 10;
        enableDynamicShardSizing();
        when(indices.indexCreationDate(INDEX_0)).thenReturn(Optional.of(clock.nowUTC()));
        when(clusterAdapter.nodesHostInfo()).thenReturn(Map.of("n1", node(dataSizeGigabytes), "n2", node(20)));
        when(indices.getStoreSizeInBytes(INDEX_0)).thenReturn(Optional.of(Size.gigabytes(dataSizeGigabytes).toBytes()));

        final IndexRotator.Result result = underTest.calculate(INDEX_0, DEFAULT_LIFETIME, indexSetConfig);

        assertThat(result.shouldRotate()).isTrue();
        assertThat(result.getDescription()).contains("Calculated dynamic shard size <10.0 GiB * 0.6 ≈ 6.0 GiB>");
    }

    @Test
    void shouldRotateWhenTooBig() {
        when(indices.indexCreationDate(INDEX_0)).thenReturn(Optional.of(clock.nowUTC()));
        when(indices.getStoreSizeInBytes(INDEX_0)).thenReturn(Optional.of(SHARD_SIZE_MIN_MAX.toBytes() + 10));

        final IndexRotator.Result result = underTest.calculate(INDEX_0, DEFAULT_LIFETIME, indexSetConfig);

        assertThat(result.shouldRotate()).isTrue();
        assertThat(result.getDescription()).contains("exceeds maximum size");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2020-12-01T09:00:00Z", "now"})
    void shouldRotateWhenRightSizedAndOverRotationPeriod(String startDate) {
        setClockTo(startDate);

        elasticsearchConfiguration.setTimeSizeOptimizingRotationMaxShardSize(Size.bytes(SHARD_SIZE_MIN_MAX.toBytes() * 2));
        when(indices.getStoreSizeInBytes(INDEX_0)).thenReturn(Optional.of(SHARD_SIZE_MIN_MAX.toBytes() + 10));
        when(indices.indexCreationDate(INDEX_0)).thenReturn(Optional.of(clock.nowUTC().minus(ROTATION_PERIOD)));

        final IndexRotator.Result result = underTest.calculate(INDEX_0, DEFAULT_LIFETIME, indexSetConfig);

        assertThat(result.shouldRotate()).isTrue();
        assertThat(result.getDescription()).contains("has passed rotation period");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2020-12-01T09:00:00Z", "now"})
    void shouldRotateWhenBeyondLeeWayForLegacyStrategies(String startDate) {
        setClockTo(startDate);

        final Period leeWay = DEFAULT_LIFETIME.indexLifetimeMax().minus(DEFAULT_LIFETIME.indexLifetimeMin());
        final Days leewayDays = Days.days(leeWay.getDays());
        final DateTime creationDate = clock.nowUTC().minus(ROTATION_PERIOD.plus(leewayDays))
                .minus(Duration.millis(10)); // avoid race between the time of now here and in code

        when(indices.indexCreationDate(INDEX_0)).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes(INDEX_0)).thenReturn(Optional.of(5L));

        final IndexRotator.Result result = underTest.calculate(INDEX_0, DEFAULT_LIFETIME, indexSetConfig);

        assertThat(result.shouldRotate()).isTrue();
        assertThat(result.getDescription()).contains("exceeds optimization leeway");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2020-12-01T09:00:00Z", "now"})
    void shouldRotateWhenBeyondLeeWayForDataTiering(String startDate) {
        setClockTo(startDate);

        final Period leeWay = DEFAULT_LIFETIME.indexLifetimeMax().minus(DEFAULT_LIFETIME.indexLifetimeMin());
        final Days leewayDays = Days.days(leeWay.getDays());
        final DateTime creationDate = clock.nowUTC().minus(ROTATION_PERIOD.plus(leewayDays))
                .minus(Duration.millis(10)); // avoid race between the time of now here and in code

        when(indexSetConfig.dataTieringConfig()).thenReturn(mock(DataTieringConfig.class));
        lenient().when(indexSetConfig.retentionStrategyConfig()).thenReturn(NoopRetentionStrategyConfig.createDefault());
        when(indices.indexCreationDate(INDEX_0)).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes(INDEX_0)).thenReturn(Optional.of(5L));

        final IndexRotator.Result result = underTest.calculate(INDEX_0, DEFAULT_LIFETIME, indexSetConfig);

        assertThat(result.shouldRotate()).isTrue();
        assertThat(result.getDescription()).contains("exceeds optimization leeway");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2020-12-01T09:00:00Z", "now"})
    void shouldIgnoreLeeWayWhenNoopRetention(String startDate) {
        setClockTo(startDate);

        final Period leeWay = DEFAULT_LIFETIME.indexLifetimeMax().minus(DEFAULT_LIFETIME.indexLifetimeMin());
        final Days leewayDays = Days.days(leeWay.getDays());
        final DateTime creationDate = clock.nowUTC().minus(
                        elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod().plus(leewayDays))
                .minus(Duration.millis(10)); // avoid race between the time of now here and in code

        when(indices.indexCreationDate(INDEX_0)).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes(INDEX_0)).thenReturn(Optional.of(5L));
        when(indexSetConfig.retentionStrategyConfig()).thenReturn(NoopRetentionStrategyConfig.createDefault());

        final IndexRotator.Result result = underTest.calculate(INDEX_0, DEFAULT_LIFETIME, indexSetConfig);

        assertThat(result.shouldRotate()).isFalse();
    }


    private void setClockTo(String startDate) {
        if (startDate.equals("now")) {
            clock.setTime(DateTime.now(DateTimeZone.UTC));
        } else {
            clock.setTime(DateTime.parse(startDate));
        }
    }

    private void enableDynamicShardSizing() {
        elasticsearchConfiguration.setTimeSizeOptimizingRotationMaxShardSize(null);
        elasticsearchConfiguration.setTimeSizeOptimizingRotationMinShardSize(null);
    }

    private static NodeOSInfo node(int gigabytes) {
        return new NodeOSInfo(Size.gigabytes(gigabytes).toBytes(), List.of("data"));
    }
}
