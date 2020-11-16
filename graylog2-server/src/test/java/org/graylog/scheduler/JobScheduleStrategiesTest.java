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
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class JobScheduleStrategiesTest {
    private JobSchedulerTestClock clock;
    private JobScheduleStrategies strategies;

    @Before
    public void setUp() throws Exception {
        this.clock = new JobSchedulerTestClock(DateTime.now(DateTimeZone.UTC));
        this.strategies = new JobScheduleStrategies(clock);
    }

    @Test
    public void nextTime() {
        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .jobDefinitionId("abc-123")
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
    public void nextFutureTime() {
        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .jobDefinitionId("abc-123")
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
