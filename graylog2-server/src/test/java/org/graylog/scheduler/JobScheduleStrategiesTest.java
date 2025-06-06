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
package org.graylog.scheduler;

import org.graylog.events.JobSchedulerTestClock;
import org.graylog.scheduler.schedule.CronJobSchedule;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class JobScheduleStrategiesTest {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");

    private JobSchedulerTestClock clock;
    private JobScheduleStrategies strategies;

    @Before
    public void setUp() throws Exception {
        DateTime dateTime = DateTime.parse("13/06/2022 15:13:59", DATE_FORMAT);
        DateTime dateTimeWithZone = dateTime.withZone(DateTimeZone.forID("UTC"));
        this.clock = new JobSchedulerTestClock(dateTimeWithZone);
        this.strategies = new JobScheduleStrategies(clock);
    }

    @Test
    public void nextTime() {
        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build();

        final DateTime nextFutureTime1 = strategies.nextTime(trigger).orElse(null);

        assertThat(nextFutureTime1)
                .isNotNull()
                .isGreaterThanOrEqualTo(clock.nowUTC())
                .isEqualByComparingTo(clock.nowUTC().plusSeconds(1));

        clock.plus(10, TimeUnit.SECONDS);

        final DateTime nextFutureTime2 = strategies.nextTime(trigger).orElse(null);

        assertThat(nextFutureTime2)
                .isNotNull()
                .isEqualByComparingTo(trigger.nextTime().plusSeconds(1));
    }

    @Test
    public void nextTimeCron() {
        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(CronJobSchedule.builder()
                        .cronExpression("0 0 1 * * ? *")
                        .timezone("EST")
                        .build())
                .build();

        final DateTime nextTime = strategies.nextTime(trigger).orElse(null);

        assertThat(nextTime)
                .isNotNull()
                .satisfies(dateTime ->  {
                    // EST is mapped to a fixed offset without daylight savings: https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html#SHORT_IDS
                    assertThat(dateTime.getZone()).isEqualTo(DateTimeZone.forID("-05:00"));
                    assertThat(dateTime.toString(DATE_FORMAT)).isEqualTo("14/06/2022 01:00:00");
                });
    }

    @Test
    public void cronNextTimeAfter() {
        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(CronJobSchedule.builder()
                        .cronExpression("0 0 * ? * * *")
                        .timezone("UTC")
                        .build())
                .build();

        DateTime date = DateTime.parse("2024-01-01T0:00:00.000Z");

        DateTime nextTime = strategies.nextTime(trigger, date).orElse(null);

        assertThat(nextTime)
                .isNotNull()
                .satisfies(dateTime -> {
                    assertThat(dateTime.getZone()).isEqualTo(DateTimeZone.forID("UTC"));
                    assertThat(dateTime.toString(DATE_FORMAT)).isEqualTo("01/01/2024 01:00:00");
                });

        date = DateTime.parse("2024-02-01T0:00:00.000Z");
        nextTime = strategies.nextTime(trigger, date).orElse(null);
        assertThat(nextTime)
                .isNotNull()
                .satisfies(dateTime -> {
                    assertThat(dateTime.getZone()).isEqualTo(DateTimeZone.forID("UTC"));
                    assertThat(dateTime.toString(DATE_FORMAT)).isEqualTo("01/02/2024 01:00:00");
                });
    }

    @Test
    public void intervalNextTimeAfter() {
        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build();

        DateTime date = DateTime.parse("2024-01-01T0:00:00.000Z");
        DateTime nextTime = strategies.nextTime(trigger, date).orElse(null);
        assertThat(nextTime)
                .isNotNull()
                .satisfies(dateTime -> {
                    assertThat(dateTime.getZone()).isEqualTo(DateTimeZone.forID("UTC"));
                    assertThat(dateTime.toString(DATE_FORMAT)).isEqualTo("01/01/2024 00:00:01");
                });

        date = DateTime.parse("2024-02-01T0:00:00.000Z");
        nextTime = strategies.nextTime(trigger, date).orElse(null);
        assertThat(nextTime)
                .isNotNull()
                .satisfies(dateTime -> {
                    assertThat(dateTime.getZone()).isEqualTo(DateTimeZone.forID("UTC"));
                    assertThat(dateTime.toString(DATE_FORMAT)).isEqualTo("01/02/2024 00:00:01");
                });
    }

    @Test
    public void emptyNextTimeCron() {
        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(CronJobSchedule.builder()
                        // At every hour in 2024
                        .cronExpression("0 0 * ? * * 2024")
                        .build())
                .build();

        // Last execution for the expression
        final DateTime date = DateTime.parse("2024-12-31T23:00:00.000Z");
        final Optional<DateTime> nextTime = strategies.nextTime(trigger, date);

        assertThat(nextTime).isEmpty();
    }

    @Test
    public void nextFutureTime() {
        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build();

        final DateTime nextFutureTime1 = strategies.nextFutureTime(trigger).orElse(null);

        assertThat(nextFutureTime1)
                .isNotNull()
                .isGreaterThanOrEqualTo(clock.nowUTC())
                .isEqualByComparingTo(clock.nowUTC().plusSeconds(1));

        clock.plus(10, TimeUnit.SECONDS);

        final DateTime nextFutureTime2 = strategies.nextFutureTime(trigger).orElse(null);

        assertThat(nextFutureTime2)
                .isNotNull()
                .isGreaterThanOrEqualTo(clock.nowUTC())
                .isEqualByComparingTo(clock.nowUTC().plusSeconds(1));
    }
}
