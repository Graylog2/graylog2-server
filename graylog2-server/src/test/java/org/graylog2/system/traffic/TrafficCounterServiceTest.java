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
package org.graylog2.system.traffic;

import com.google.common.collect.ImmutableList;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class TrafficCounterServiceTest {

    private final NodeId nodeId = new SimpleNodeId("node-1");

    private TrafficCounterService service;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        service = new TrafficCounterService(mongoCollections);
    }

    static DateTime getDayBucket(DateTime time) {
        return time.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
    }

    @Test
    void updateTrafficAndReadPerDay() {
        // Make sure we use a fixed time for the test
        final DateTime now = DateTime.parse("2017-10-29T08:20:00.000Z");
        DateTimeUtils.setCurrentMillisProvider(new InstantMillisProvider(now));

        try {
            final DateTime today = getDayBucket(now);

            // Record traffic for each 10-minute interval of the current day
            // At 08:20, we have completed 50 intervals (0-49) and are in the 51st interval
            // 8 hours * 6 (intervals/hour) + 2 intervals (00, 10, 20) = 51 intervals total (0-50)
            int currentInterval = (now.hourOfDay().get() * 6) + (now.minuteOfHour().get() / 10);
            IntStream.rangeClosed(0, currentInterval).forEach(interval ->
                    service.updateTraffic(today.plusMinutes(interval * 10), nodeId, 1, 1, 1));

            // Record traffic for all previous days - 30 days x 144 ten-minute intervals per day
            IntStream.rangeClosed(1, 30).forEach(day ->
                    IntStream.rangeClosed(0, 143).forEach(interval ->
                            service.updateTraffic(today.minusDays(day).plusMinutes(interval * 10), nodeId, 1, 1, 1)));

            // Verify that today is included from the histogram.
            final TrafficCounterService.TrafficHistogram trafficHistogramIncludesToday =
                    service.clusterTrafficOfLastDays(Duration.standardDays(30), TrafficCounterService.Interval.DAILY, true);
            assertThat(trafficHistogramIncludesToday.from()).isEqualTo(getDayBucket(now).minusDays(30));
            assertThat(trafficHistogramIncludesToday.to()).isEqualTo(now);
            assertThat(trafficHistogramIncludesToday.input()).hasSize(31);
            assertThat(trafficHistogramIncludesToday.decoded()).hasSize(31);
            assertThat(trafficHistogramIncludesToday.output()).hasSize(31);
            verifyDayTrafficVolume(trafficHistogramIncludesToday);

            // Verify that today is omitted from the histogram.
            final TrafficCounterService.TrafficHistogram trafficHistogramExcludesToday =
                    service.clusterTrafficOfLastDays(
                            Duration.standardDays(30),
                            TrafficCounterService.Interval.DAILY, false);
            assertThat(trafficHistogramExcludesToday.from()).isEqualTo(getDayBucket(now).minusDays(30));
            assertThat(trafficHistogramExcludesToday.to()).isEqualTo(getDayBucket(now).minusMillis(1));
            assertThat(trafficHistogramExcludesToday.input()).hasSize(30);
            assertThat(trafficHistogramExcludesToday.decoded()).hasSize(30);
            assertThat(trafficHistogramExcludesToday.output()).hasSize(30);
            verifyDayTrafficVolume(trafficHistogramIncludesToday);
        } finally {
            DateTimeUtils.setCurrentMillisSystem();
        }
    }

    private static void verifyDayTrafficVolume(TrafficCounterService.TrafficHistogram trafficHistogram) {
        // For each type of traffic, check that we got the correct values
        ImmutableList.of(trafficHistogram.input(), trafficHistogram.decoded(), trafficHistogram.output()).forEach(histogram -> {
            final ImmutableList<Long> outputValues = ImmutableList.copyOf(histogram.values());

            // Check that we got the expected count for each of the previous days. We should get the full counter
            // for the complete day. (144 ten-minute intervals per day)
            for (int i = 0; i < 30; i++) {
                assertThat(outputValues.get(i))
                        .withFailMessage("Value <%s> is not the expected value - expected=%s but got=%s",
                                i, 144, outputValues.get(i))
                        .isEqualTo(144);
            }

            // Check that we got the correct count for the current day. At 08:20, we have 51 ten-minute intervals
            // (8 hours * 6 intervals/hour + 2 intervals for 00, 10, 20 = 51 intervals)
            assertThat(outputValues.get(30))
                    .withFailMessage("Value <%s> is not the expected value - expected=%s but got=%s",
                            30, 51, outputValues.get(30))
                    .isEqualTo(51);
        });
    }

    @Test
    void updateTrafficAndReadPerTenMinutes() {
        // Make sure we use a fixed time for the test
        final DateTime now = DateTime.parse("2017-10-29T08:20:00.000Z");
        DateTimeUtils.setCurrentMillisProvider(new InstantMillisProvider(now));

        try {
            final DateTime today = getDayBucket(now);

            // Record traffic for each 10-minute interval of the current day
            int currentInterval = (now.hourOfDay().get() * 6) + (now.minuteOfHour().get() / 10);
            IntStream.rangeClosed(0, currentInterval).forEach(interval ->
                    service.updateTraffic(today.plusMinutes(interval * 10), nodeId, 1, 1, 1));

            // Record traffic for all previous days - 30 days x 144 ten-minute intervals per day
            IntStream.rangeClosed(1, 30).forEach(day ->
                    IntStream.rangeClosed(0, 143).forEach(interval ->
                            service.updateTraffic(today.minusDays(day).plusMinutes(interval * 10), nodeId, 1, 1, 1)));

            final TrafficCounterService.TrafficHistogram histogramIncludesToday =
                    service.clusterTrafficOfLastDays(Duration.standardDays(30), TrafficCounterService.Interval.TEN_MINUTE, true);
            assertThat(histogramIncludesToday.from()).isEqualTo(getDayBucket(now).minusDays(30));
            assertThat(histogramIncludesToday.to()).isEqualTo(now);

            // We should have 4371 entries: 30 days (30x144 = 4320) + current day (51 ten-minute intervals)
            assertThat(histogramIncludesToday.input()).hasSize(4371);
            assertThat(histogramIncludesToday.decoded()).hasSize(4371);
            assertThat(histogramIncludesToday.output()).hasSize(4371);
            verifyTenMinuteTraffic(histogramIncludesToday, 4371);

            final TrafficCounterService.TrafficHistogram histogramExcludesToday =
                    service.clusterTrafficOfLastDays(Duration.standardDays(30), TrafficCounterService.Interval.TEN_MINUTE, false);
            assertThat(histogramExcludesToday.from()).isEqualTo(getDayBucket(now).minusDays(30));
            assertThat(histogramExcludesToday.to()).isEqualTo(getDayBucket(now).minusMillis(1));

            // We should have 4320 entries: 30 days x 144 ten-minute intervals per day (no current day)
            assertThat(histogramExcludesToday.input()).hasSize(4320);
            assertThat(histogramExcludesToday.decoded()).hasSize(4320);
            assertThat(histogramExcludesToday.output()).hasSize(4320);
            verifyTenMinuteTraffic(histogramExcludesToday, 4320);
        } finally {
            DateTimeUtils.setCurrentMillisSystem();
        }
    }

    @Test
    void updateTrafficAndReadPerHour() {
        // Make sure we use a fixed time for the test
        final DateTime now = DateTime.parse("2017-10-29T08:20:00.000Z");
        DateTimeUtils.setCurrentMillisProvider(new InstantMillisProvider(now));

        try {
            final DateTime today = getDayBucket(now);

            // Record traffic for each 10-minute interval of the current day
            int currentInterval = (now.hourOfDay().get() * 6) + (now.minuteOfHour().get() / 10);
            IntStream.rangeClosed(0, currentInterval).forEach(interval ->
                    service.updateTraffic(today.plusMinutes(interval * 10), nodeId, 1, 1, 1));

            // Record traffic for all previous days - 30 days x 144 ten-minute intervals per day
            IntStream.rangeClosed(1, 30).forEach(day ->
                    IntStream.rangeClosed(0, 143).forEach(interval ->
                            service.updateTraffic(today.minusDays(day).plusMinutes(interval * 10), nodeId, 1, 1, 1)));

            // Test HOURLY aggregation - should aggregate 10-minute buckets into hourly buckets
            final TrafficCounterService.TrafficHistogram histogramIncludesToday =
                    service.clusterTrafficOfLastDays(Duration.standardDays(30), TrafficCounterService.Interval.HOURLY, true);
            assertThat(histogramIncludesToday.from()).isEqualTo(getDayBucket(now).minusDays(30));
            assertThat(histogramIncludesToday.to()).isEqualTo(now);

            // We should have 729 entries: 30 days (30x24 = 720) + current day (9 hours: 0-8)
            // Each hour aggregates 6 ten-minute intervals
            assertThat(histogramIncludesToday.input()).hasSize(729);
            assertThat(histogramIncludesToday.decoded()).hasSize(729);
            assertThat(histogramIncludesToday.output()).hasSize(729);
            verifyHourlyAggregatedTraffic(histogramIncludesToday, 729, true);

            final TrafficCounterService.TrafficHistogram histogramExcludesToday =
                    service.clusterTrafficOfLastDays(Duration.standardDays(30), TrafficCounterService.Interval.HOURLY, false);
            assertThat(histogramExcludesToday.from()).isEqualTo(getDayBucket(now).minusDays(30));
            assertThat(histogramExcludesToday.to()).isEqualTo(getDayBucket(now).minusMillis(1));

            // We should have 720 entries: 30 days x 24 hours per day (no current day)
            assertThat(histogramExcludesToday.input()).hasSize(720);
            assertThat(histogramExcludesToday.decoded()).hasSize(720);
            assertThat(histogramExcludesToday.output()).hasSize(720);
            verifyHourlyAggregatedTraffic(histogramExcludesToday, 720, false);
        } finally {
            DateTimeUtils.setCurrentMillisSystem();
        }
    }

    private static void verifyTenMinuteTraffic(TrafficCounterService.TrafficHistogram trafficHistogram, int bucketCount) {
        // For each type of traffic, check that we got the correct values
        ImmutableList.of(trafficHistogram.input(), trafficHistogram.decoded(), trafficHistogram.output()).forEach(histogram -> {
            final ImmutableList<Long> outputValues = ImmutableList.copyOf(histogram.values());

            // Check that we got the expected count for each 10-minute interval. We should get one value per interval. (1)
            for (int i = 0; i < bucketCount; i++) {
                assertThat(outputValues.get(i))
                        .withFailMessage("Value <%s> is not the expected value - expected=%s but got=%s",
                                i, 1, outputValues.get(i))
                        .isEqualTo(1);
            }
        });
    }

    private static void verifyHourlyAggregatedTraffic(TrafficCounterService.TrafficHistogram trafficHistogram,
                                                      int bucketCount,
                                                      boolean includesPartialCurrentHour) {
        // For each type of traffic, check that we got the correct values
        ImmutableList.of(trafficHistogram.input(), trafficHistogram.decoded(), trafficHistogram.output()).forEach(histogram -> {
            final ImmutableList<Long> outputValues = ImmutableList.copyOf(histogram.values());

            if (includesPartialCurrentHour) {
                // Check that we got the expected count for each complete hourly bucket
                // Each complete hour should have 6 (ten-minute intervals aggregated)
                for (int i = 0; i < bucketCount - 1; i++) {
                    assertThat(outputValues.get(i))
                            .withFailMessage("Value <%s> is not the expected value - expected=%s but got=%s",
                                    i, 6, outputValues.get(i))
                            .isEqualTo(6);
                }

                // The last bucket (current hour 08:00-08:59 at 08:20) has only 3 intervals: 08:00, 08:10, 08:20
                assertThat(outputValues.get(bucketCount - 1))
                        .withFailMessage("Value <%s> is not the expected value - expected=%s but got=%s",
                                bucketCount - 1, 3, outputValues.get(bucketCount - 1))
                        .isEqualTo(3);
            } else {
                // All hours are complete, each should have 6 ten-minute intervals
                for (int i = 0; i < bucketCount; i++) {
                    assertThat(outputValues.get(i))
                            .withFailMessage("Value <%s> is not the expected value - expected=%s but got=%s",
                                    i, 6, outputValues.get(i))
                            .isEqualTo(6);
                }
            }
        });
    }
}
