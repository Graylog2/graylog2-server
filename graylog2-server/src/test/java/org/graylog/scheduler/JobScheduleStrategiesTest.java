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
                        .timezone("Europe/Vienna")
                        .build())
                .build();

        final DateTime nextTime = strategies.nextTime(trigger).orElse(null);

        assertThat(nextTime)
                .isNotNull()
                .satisfies(dateTime ->  {
                    assertThat(dateTime.getZone()).isEqualTo(DateTimeZone.forID("Europe/Vienna"));
                    assertThat(dateTime.toString(DATE_FORMAT)).isEqualTo("14/06/2022 01:00:00");
                });
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
