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

import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Manages schedules for system jobs.
 */
@Singleton
public class ScheduledSystemJobManager extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledSystemJobManager.class);

    private final Map<String, SystemJobScheduleProvider<? extends SystemJobConfig>> scheduleProviders;
    private final JobSchedulerClock clock;
    private final SystemJobManager manager;

    @Inject
    public ScheduledSystemJobManager(Map<String, SystemJobScheduleProvider<? extends SystemJobConfig>> scheduleProviders,
                                     JobSchedulerClock clock,
                                     SystemJobManager manager) {
        this.scheduleProviders = scheduleProviders;
        this.clock = clock;
        this.manager = manager;
    }

    @Override
    protected void startUp() throws Exception {
        for (final var entry : scheduleProviders.entrySet()) {
            final var config = entry.getValue().getConfig();
            final var schedule = entry.getValue().getSchedule();
            final var firstTime = clock.instantNow(); // TODO: Is now okay our should we calculate the firstTime based on the schedule?

            schedule.ifPresent(jobSchedule -> manager.submitWithSchedule(config, jobSchedule, firstTime));
        }
    }

    public void updateSchedule(String type, Instant nextTime) throws Exception {
        final var provider = scheduleProviders.get(type);

        if (provider != null) {
            final var config = provider.getConfig();
            final var schedule = provider.getSchedule();

            schedule.ifPresent(jobSchedule -> manager.submitWithSchedule(config, jobSchedule, nextTime));
        }
    }

    @Override
    protected void shutDown() throws Exception {
        // Nothing to do
    }
}
