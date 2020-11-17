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

import org.graylog.scheduler.clock.JobSchedulerClock;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Provides a few standard schedule strategies for triggers.
 */
public class JobScheduleStrategies {
    private static final Logger LOG = LoggerFactory.getLogger(JobScheduleStrategies.class);

    private final JobSchedulerClock clock;

    @Inject
    public JobScheduleStrategies(JobSchedulerClock clock) {
        this.clock = clock;
    }

    /**
     * Calculates the next execution time. This uses the previous {@link JobTriggerDto#nextTime()} to calculate
     * the next one based on the trigger schedule.
     * <p>
     * If this returns an empty {@link Optional}, the trigger should not be executed anymore.
     *
     * @param trigger the trigger to use for the calculation
     * @return the next time this trigger should fire, empty optional if the trigger should not fire anymore
     */
    public Optional<DateTime> nextTime(JobTriggerDto trigger) {
        final DateTime lastNextTime = trigger.nextTime();
        final DateTime lastExecutionTime = trigger.lock().lastLockTime();

        return trigger.schedule().calculateNextTime(lastExecutionTime, lastNextTime);
    }

    /**
     * Calculates the next time in the future. This uses the previous {@link JobTriggerDto#nextTime()} to calculate
     * the next one based on the trigger schedule. It recalculates the next time until it is in the future.
     * <p>
     * If this returns an empty {@link Optional}, the trigger should not be executed anymore.
     *
     * @param trigger the trigger to use for the calculation
     * @return the next time this trigger should fire, empty optional if the trigger should not fire anymore
     */
    public Optional<DateTime> nextFutureTime(JobTriggerDto trigger) {
        final DateTime now = clock.nowUTC();
        final DateTime lastNextTime = trigger.nextTime();
        final DateTime lastExecutionTime = trigger.lock().lastLockTime();
        final JobSchedule schedule = trigger.schedule();

        // This is using nextTime to make sure we take the runtime into account and schedule at
        // exactly after the last nextTime.
        final Optional<DateTime> optionalNextTime = schedule.calculateNextTime(lastExecutionTime, lastNextTime);

        if (!optionalNextTime.isPresent()) {
            return Optional.empty();
        }

        DateTime nextTime = optionalNextTime.get();

        // If calculated nextTime is in the past, calculate next time until it is in the future
        // TODO: Is this something we should notify the user about? If a job is using this helper method it probably
        //       doesn't care about this situation. Jobs where it's important that the time doesn't automatically
        //       advance, should probably use a different helper method.
        while (!nextTime.isAfter(now)) {
            LOG.debug("New nextTime <{}> is in the past, re-calculating again", nextTime);
            nextTime = schedule.calculateNextTime(lastExecutionTime, nextTime).orElse(null);
            if (nextTime == null) {
                return Optional.empty();
            }
        }

        return Optional.of(nextTime);
    }
}
