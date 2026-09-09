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
package org.graylog.scheduler.system;

import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SystemJobResultTest {
    private final JobTriggerDto trigger = JobTriggerDto.builder()
            .jobDefinitionId("test-job")
            .jobDefinitionType(SystemJobDefinitionConfig.TYPE_NAME)
            .schedule(OnceJobSchedule.create())
            .nextTime(DateTime.now(DateTimeZone.UTC))
            .build();

    @Test
    void successCompletesTheTrigger() {
        final var update = SystemJobResult.Converter.toJobTriggerUpdate(SystemJobResult.success(), trigger);

        assertThat(update.nextTime()).isEmpty();
        assertThat(update.status()).isEmpty(); // An empty next time completes the trigger
    }

    @Test
    void cancelledKeepsTheCancelledStatus() {
        final var update = SystemJobResult.Converter.toJobTriggerUpdate(SystemJobResult.cancelled(), trigger);

        assertThat(update.nextTime()).isEmpty();
        assertThat(update.status()).contains(JobTriggerStatus.CANCELLED);
    }

    @Test
    void errorKeepsTheErrorStatus() {
        final var update = SystemJobResult.Converter.toJobTriggerUpdate(SystemJobResult.withError(), trigger);

        assertThat(update.status()).contains(JobTriggerStatus.ERROR);
    }

    @Test
    void retryReschedulesTheTrigger() {
        final var result = SystemJobResult.withRetry(Duration.ofSeconds(5), Integer.MAX_VALUE);
        final var update = SystemJobResult.Converter.toJobTriggerUpdate(result, trigger);

        assertThat(update.nextTime()).isPresent();
        assertThat(update.status()).isEmpty(); // A next time without status makes the trigger RUNNABLE again
    }
}
