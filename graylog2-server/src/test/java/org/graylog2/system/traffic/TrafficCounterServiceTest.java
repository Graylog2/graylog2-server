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
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(MockitoExtension.class)
class TrafficCounterServiceTest {

    @Mock
    private NodeId nodeId;

    private TrafficCounterService service;

    @BeforeEach
    void setUp(MongoDBTestService mongodb, MongoJackObjectMapperProvider objectMapperProvider) {
        lenient().when(nodeId.toEscapedString()).thenReturn("node-1");

        service = new TrafficCounterService(mongodb.mongoConnection(), objectMapperProvider);
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

            // Record traffic for each hour of the current day - 9 hours (it's 08:20, so we are in the 9th hour of the day)
            IntStream.rangeClosed(0, now.hourOfDay().get()).forEach(hour -> {
                service.updateTraffic(today.plusHours(hour), nodeId, 1, 1, 1);
            });

            // Record traffic for all previous days - 30 x 24 hours
            IntStream.rangeClosed(1, 30).forEach(day -> {
                IntStream.rangeClosed(0, 23).forEach(hour -> {
                    service.updateTraffic(today.minusDays(day).plusHours(hour), nodeId, 1, 1, 1);
                });
            });

            final TrafficCounterService.TrafficHistogram trafficHistogram = service.clusterTrafficOfLastDays(
                    Duration.standardDays(30),
                    TrafficCounterService.Interval.DAILY
            );

            assertThat(trafficHistogram.from()).isEqualTo(getDayBucket(now).minusDays(30));
            assertThat(trafficHistogram.to()).isEqualTo(now);

            // We should have 31 entries, 30 days history and the current day
            assertThat(trafficHistogram.input()).hasSize(31);
            assertThat(trafficHistogram.decoded()).hasSize(31);
            assertThat(trafficHistogram.output()).hasSize(31);

            // For each type of traffic, check that we got the correct values
            ImmutableList.of(trafficHistogram.input(), trafficHistogram.decoded(), trafficHistogram.output()).forEach(histogram -> {
                final ImmutableList<Long> outputValues = ImmutableList.copyOf(histogram.values());

                // Check that we got the expected count for each of the previous days. We should get the full counter
                // for the complete day. (24)
                for (int i = 0; i < 30; i++) {
                    assertThat(outputValues.get(i))
                            .withFailMessage("Value <%s> is not the expected value - expected=%s but got=%s",
                                    i, 24, outputValues.get(i))
                            .isEqualTo(24);
                }

                // Check that we got the correct count for the current day. The current day is only in its 9th hour,
                // so we should only get a value of 9.
                assertThat(outputValues.get(30))
                        .withFailMessage("Value <%s> is not the expected value - expected=%s but got=%s",
                                30, 9, outputValues.get(30))
                        .isEqualTo(9);
            });
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

            // Record traffic for each hour of the current day (now)
            IntStream.rangeClosed(0, now.hourOfDay().get()).forEach(hour -> {
                service.updateTraffic(today.plusHours(hour), nodeId, 1, 1, 1);
            });

            // Record traffic for all previous days
            IntStream.rangeClosed(1, 30).forEach(day -> {
                IntStream.rangeClosed(0, 23).forEach(hour -> {
                    service.updateTraffic(today.minusDays(day).plusHours(hour), nodeId, 1, 1, 1);
                });
            });

            final TrafficCounterService.TrafficHistogram trafficHistogram = service.clusterTrafficOfLastDays(
                    Duration.standardDays(30),
                    TrafficCounterService.Interval.HOURLY
            );

            assertThat(trafficHistogram.from()).isEqualTo(getDayBucket(now).minusDays(30));
            assertThat(trafficHistogram.to()).isEqualTo(now);

            // We should have 729 entries, one for each hour in the 30 days (30x24) history and the current day (9 hours)
            assertThat(trafficHistogram.input()).hasSize(729);
            assertThat(trafficHistogram.decoded()).hasSize(729);
            assertThat(trafficHistogram.output()).hasSize(729);

            // For each type of traffic, check that we got the correct values
            ImmutableList.of(trafficHistogram.input(), trafficHistogram.decoded(), trafficHistogram.output()).forEach(histogram -> {
                final ImmutableList<Long> outputValues = ImmutableList.copyOf(histogram.values());

                // Check that we got the expected count for each of the previous days. We should get one value per hour. (1)
                for (int i = 0; i < 729; i++) {
                    assertThat(outputValues.get(i))
                            .withFailMessage("Value <%s> is not the expected value - expected=%s but got=%s",
                                    i, 1, outputValues.get(i))
                            .isEqualTo(1);
                }
            });
        } finally {
            DateTimeUtils.setCurrentMillisSystem();
        }
    }
}
