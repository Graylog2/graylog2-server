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
package org.graylog.scheduler.rest.requests;

import org.graylog.events.JobSchedulerTestClock;
import org.graylog.scheduler.JobTriggerData;
import org.graylog.scheduler.JobTriggerLock;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;

public class CreateJobTriggerRequestTest {
    @Test
    public void toDto() {
        final DateTime now = DateTime.now(UTC);
        final JobSchedulerTestClock clock = new JobSchedulerTestClock(now);

        final IntervalJobSchedule schedule = IntervalJobSchedule.builder()
                .interval(1)
                .unit(TimeUnit.SECONDS)
                .build();

        final CreateJobTriggerRequest request = CreateJobTriggerRequest.builder()
                .jobDefinitionId("abc-123")
                .startTime(now)
                .nextTime(now)
                .schedule(schedule)
                .build();

        final JobTriggerData.FallbackData data = new JobTriggerData.FallbackData();
        final CreateJobTriggerRequest requestWithDataAndEndTime = CreateJobTriggerRequest.builder()
                .jobDefinitionId("abc-123")
                .startTime(now)
                .endTime(now.plusDays(1))
                .nextTime(now)
                .schedule(schedule)
                .data(data)
                .build();

        assertThat(request.toDto(clock)).satisfies(dto -> {
            assertThat(dto.jobDefinitionId()).isEqualTo("abc-123");
            assertThat(dto.startTime()).isEqualTo(now);
            assertThat(dto.endTime()).isNotPresent();
            assertThat(dto.nextTime()).isEqualTo(now);
            assertThat(dto.createdAt()).isEqualTo(now);
            assertThat(dto.updatedAt()).isEqualTo(now);
            assertThat(dto.triggeredAt()).isNotPresent();
            assertThat(dto.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
            assertThat(dto.lock()).isEqualTo(JobTriggerLock.empty());
            assertThat(dto.schedule()).isEqualTo(schedule);
            assertThat(dto.data()).isNotPresent();
        });

        assertThat(requestWithDataAndEndTime.toDto(clock)).satisfies(dto -> {
            assertThat(dto.jobDefinitionId()).isEqualTo("abc-123");
            assertThat(dto.startTime()).isEqualTo(now);
            assertThat(dto.endTime()).isPresent().get().isEqualTo(now.plusDays(1));
            assertThat(dto.nextTime()).isEqualTo(now);
            assertThat(dto.createdAt()).isEqualTo(now);
            assertThat(dto.updatedAt()).isEqualTo(now);
            assertThat(dto.triggeredAt()).isNotPresent();
            assertThat(dto.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
            assertThat(dto.lock()).isEqualTo(JobTriggerLock.empty());
            assertThat(dto.schedule()).isEqualTo(schedule);
            assertThat(dto.data()).isPresent().get().isEqualTo(data);
        });
    }
}
