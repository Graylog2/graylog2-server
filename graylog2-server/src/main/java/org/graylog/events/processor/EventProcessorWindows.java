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
package org.graylog.events.processor;

import jakarta.inject.Inject;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog.scheduler.JobScheduleStrategies;
import org.graylog.scheduler.JobTriggerDto;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Computes the next time window of an event processor job after a successful run, including catch-up. Shared by
 * {@link EventProcessorExecutionJob} and plugin jobs that run event definitions.
 */
public class EventProcessorWindows {
    // Log under the job's name so existing log settings for EventProcessorExecutionJob keep working
    private static final Logger LOG = LoggerFactory.getLogger(EventProcessorExecutionJob.class);

    private final JobScheduleStrategies scheduleStrategies;
    private final EventsConfigurationProvider configurationProvider;

    /**
     * @param from     start of the next window
     * @param to       end of the next window
     * @param nextTime when to run next; empty if the trigger has no further executions (ONCE trigger)
     */
    public record NextWindow(DateTime from, DateTime to, Optional<DateTime> nextTime) {}

    @Inject
    public EventProcessorWindows(JobScheduleStrategies scheduleStrategies,
                                 EventsConfigurationProvider configurationProvider) {
        this.scheduleStrategies = scheduleStrategies;
        this.configurationProvider = configurationProvider;
    }

    /**
     * @param trigger the trigger that just ran
     * @param config  the job definition config
     * @param to      end of the window that just ran
     * @param now     the current time
     */
    public NextWindow next(JobTriggerDto trigger, EventProcessorJobConfig config, DateTime to, DateTime now) {
        // By using the processingWindowSize and the processingHopSize we can implement hopping and tumbling
        // windows. (a tumbling window is simply a hopping window where windowSize and hopSize are the same).
        // If the job uses cron scheduling, we need to instead calculate the nextTo field based on the current to
        // field as it is possible to skip contiguous time ranges with cron scheduling.
        DateTime nextTo = config.isCron() ?
                scheduleStrategies.nextTime(trigger, to).orElse(to.plus(config.processingHopSize())) :
                to.plus(config.processingHopSize());
        final DateTime nextFrom = nextTo.minus(config.processingWindowSize());

        // If the event processor is catching up on old data (e.g. the server was shut down for a significant time),
        // we can switch to a bigger scheduling window: `processingCatchUpWindowSize`.
        // If engaged, we will schedule jobs with a timerange of multiple processingWindowSize chunks.
        // It's the specific event processors' duty to handle being executed with this larger timerange.
        // If an event processor was configured with a processingHopSize greater than the processingWindowSize
        // we can't use the catchup mode.
        // Catch-up can be disabled per event processor via the disableCatchup flag.
        final long catchUpSize = configurationProvider.get().eventCatchupWindow();
        if (!config.disableCatchup() && !config.isCron() && catchUpSize > 0 && catchUpSize > config.processingWindowSize() && to.plus(catchUpSize).isBefore(now) &&
                config.processingHopSize() <= config.processingWindowSize()) {
            final long chunkCount = catchUpSize / config.processingWindowSize();

            // Align to multiples of the processingWindowSize
            nextTo = to.plus(config.processingWindowSize() * chunkCount);
            LOG.info("Event processor <{}> is catching up on old data. Combining {} search windows with catchUpWindowSize={}ms: from={} to={}",
                    config.eventDefinitionId(), chunkCount, catchUpSize, nextFrom, nextTo);
        }

        LOG.trace("Set new timerange of eventproc <{}> in job trigger data: from={} to={} (hopSize={}ms windowSize={}ms)",
                config.eventDefinitionId(), nextFrom, nextTo, config.processingHopSize(), config.processingWindowSize());

        final Optional<DateTime> nextTime = scheduleStrategies.nextTime(trigger);

        // The nextTime Optional can be empty if there will be no further executions of the trigger
        if (nextTime.isEmpty()) {
            LOG.trace("No nextTime for trigger <{}>", trigger.id());
            return new NextWindow(nextFrom, nextTo, Optional.empty());
        }
        if (!config.disableCatchup() && nextTo.isBefore(now)) {
            // If the next "to" timestamp of the timerange to process is in the past, we want to schedule the next
            // execution of this job as soon as possible to make sure we catch up.
            LOG.trace("Set nextTime to <{}> to catch up faster - calculated nextTime was <{}>", now, nextTime.get());
            return new NextWindow(nextFrom, nextTo, Optional.of(now));
        } else if (nextTo.isBefore(nextTime.get())) {
            LOG.trace("Set nextTime to <{}> because it's closer to the timerange time - calculated nextTime was <{}>", nextTo, nextTime.get());
            return new NextWindow(nextFrom, nextTo, Optional.of(nextTo));
        }
        // Otherwise use the calculated nextTime
        LOG.trace("Set nextTime to <{}>", nextTime.get());
        return new NextWindow(nextFrom, nextTo, nextTime);
    }
}
