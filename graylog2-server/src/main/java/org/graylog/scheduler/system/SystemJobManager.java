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

import com.google.common.primitives.Ints;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.scheduler.DBSystemJobTriggerService;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.time.Duration;

@Singleton
public class SystemJobManager {
    private final DBSystemJobTriggerService triggerService;
    private final JobSchedulerClock clock;

    @Inject
    public SystemJobManager(DBSystemJobTriggerService triggerService,
                            JobSchedulerClock clock) {
        this.triggerService = triggerService;
        this.clock = clock;
    }

    public void submit(SystemJobConfig config) {
        submitWithDelay(config, Duration.ZERO);
    }

    public void submitWithDelay(SystemJobConfig config, Duration delay) {
        final var now = DateTime.now(DateTimeZone.UTC);
        final var trigger = JobTriggerDto.builderWithClock(clock)
                .jobDefinitionType(SystemJobDefinitionConfig.TYPE_NAME)
                .jobDefinitionId(config.type())
                .data(config)
                .nextTime(now.plusMillis(Ints.saturatedCast(delay.toMillis())))
                .schedule(OnceJobSchedule.create())
                .build();

        triggerService.create(trigger);
    }
}
