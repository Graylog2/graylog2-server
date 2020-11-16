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

import com.google.common.collect.ImmutableMap;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog.events.TestJobTriggerData;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class JobTriggerUpdatesTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private JobSchedulerTestClock clock;
    private JobScheduleStrategies strategies;

    @Before
    public void setUp() throws Exception {
        this.clock = new JobSchedulerTestClock(DateTime.now(DateTimeZone.UTC));
        this.strategies = new JobScheduleStrategies(clock);
    }

    @Test
    public void scheduleNextExecution() {
        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .jobDefinitionId("abc-123")
                .schedule(IntervalJobSchedule.builder().interval(31).unit(TimeUnit.SECONDS).build())
                .build();

        final JobTriggerUpdates updates = new JobTriggerUpdates(clock, strategies, trigger);

        assertThat(updates.scheduleNextExecution()).isEqualTo(JobTriggerUpdate.withNextTime(clock.nowUTC().plusSeconds(31)));
    }

    @Test
    public void scheduleNextExecutionWithData() {
        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .jobDefinitionId("abc-123")
                .schedule(IntervalJobSchedule.builder().interval(5).unit(TimeUnit.MINUTES).build())
                .build();

        final JobTriggerUpdates updates = new JobTriggerUpdates(clock, strategies, trigger);
        final TestJobTriggerData data = TestJobTriggerData.create(ImmutableMap.of("hello", "world"));

        assertThat(updates.scheduleNextExecution(data))
                .isEqualTo(JobTriggerUpdate.withNextTimeAndData(clock.nowUTC().plusMinutes(5), data));
    }

    @Test
    public void retryIn() {
        final JobTriggerUpdates updates = new JobTriggerUpdates(clock, strategies, mock(JobTriggerDto.class));

        assertThat(updates.retryIn(123, TimeUnit.SECONDS))
                .isEqualTo(JobTriggerUpdate.withNextTime(clock.nowUTC().plusSeconds(123)));

        assertThat(updates.retryIn(1, TimeUnit.HOURS))
                .isEqualTo(JobTriggerUpdate.withNextTime(clock.nowUTC().plusHours(1)));
    }
}
