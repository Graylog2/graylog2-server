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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.Assisted;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobScheduleStrategies;
import org.graylog.scheduler.JobTriggerData;
import org.graylog.scheduler.JobTriggerUpdate;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class EventProcessorExecutionJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(EventProcessorExecutionJob.class);

    public static final String TYPE_NAME = "event-processor-execution-v1";

    public interface Factory extends Job.Factory<EventProcessorExecutionJob> {
        @Override
        EventProcessorExecutionJob create(JobDefinitionDto jobDefinition);
    }

    // TODO: Make retry interval configurable and check if we need different intervals for different error conditions
    private static final long RETRY_INTERVAL = 5000;

    private final JobScheduleStrategies scheduleStrategies;
    private final JobSchedulerClock clock;
    private final EventProcessorEngine eventProcessorEngine;
    private final Config config;
    private final EventsConfigurationProvider configurationProvider;

    @Inject
    public EventProcessorExecutionJob(JobScheduleStrategies scheduleStrategies,
                                      JobSchedulerClock clock,
                                      EventProcessorEngine eventProcessorEngine,
                                      EventsConfigurationProvider configurationProvider,
                                      @Assisted JobDefinitionDto jobDefinition) {
        this.scheduleStrategies = scheduleStrategies;
        this.clock = clock;
        this.eventProcessorEngine = eventProcessorEngine;
        this.configurationProvider = configurationProvider;
        this.config = (Config) jobDefinition.config();
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        final Optional<Data> data = ctx.trigger().data().map(d -> (Data) d);

        // Use timerange from job trigger data if it exists
        final EventProcessorParametersWithTimerange parameters;
        if (data.isPresent()) {
            LOG.trace("Using timerange from job trigger data: from={} to={} (trigger={})",
                    data.get().timerangeFrom(), data.get().timerangeTo(), ctx.trigger().id());
            parameters = config.parameters().withTimerange(data.get().timerangeFrom(), data.get().timerangeTo());
        } else {
            parameters = config.parameters();
        }

        final DateTime from = parameters.timerange().getFrom();
        final DateTime to = parameters.timerange().getTo();

        // The "to" timestamp must be after the "from" timestamp!
        if (!to.isAfter(from)) {
            // This should not happen(TM)
            // If it does, set the error status to ERROR so the scheduler doesn't try to execute it until the problem
            // has been resolved.
            // TODO: Send an event when this happens so admins can get alerted
            final JobTriggerUpdate triggerUpdate = JobTriggerUpdate.withError(ctx.trigger());
            throw new JobExecutionException("Invalid time range - \"to\" timestamp <" + to.toString() + "> is not after \"from\" timestamp <" + from.toString() + ">",
                    ctx.trigger(), triggerUpdate);
        }

        // We cannot run the event processor if the "to" timestamp of the timerange we want to process is in the future.
        final DateTime now = clock.nowUTC();
        if (now.isBefore(to)) {
            LOG.error("The end of the timerange to process is in the future, re-scheduling job trigger <{}> to run at <{}>",
                    ctx.trigger().id(), to);
            return JobTriggerUpdate.withNextTime(to);
        }

        try {
            eventProcessorEngine.execute(config.eventDefinitionId(), parameters);

            // By using the processingWindowSize and the processingHopSize we can implement hopping and tumbling
            // windows. (a tumbling window is simply a hopping window where windowSize and hopSize are the same)
            //
            // TODO: Adding a millisecond is a hack! Adjust search infrastructure to handle the time range overlap issue.
            // We are adding one millisecond to the next "from" value to avoid overlap with the previous timerange.
            // Our Elasticsearch search queries do "timestamp >= from && timestamp <= to" (inclusive), so without
            // the additional millisecond, the next "from" would be the same value as the current "to". This could
            // lead to duplicate events or (more) incorrect aggregations when processing consecutive time ranges.
            // Adding a millisecond to the next "from" value should be "okay" for now because the current date type
            // we are using in Elasticsearch is only using millisecond precision. ("date") Once we switch to a
            // different date type with nanosecond precision (e.g. "date_nanos"), this workaround will break and we
            // will miss messages.
            DateTime nextTo = to.plus(config.processingHopSize());
            DateTime nextFrom = nextTo.minus(config.processingWindowSize()).plusMillis(1);

            // If the event processor is catching up on old data (e.g. the server was shut down for a significant time),
            // we can switch to a bigger scheduling window: `processingCatchUpWindowSize`.
            // If engaged, we will schedule jobs with a timerange of multiple processingWindowSize chunks.
            // It's the specific event processors' duty to handle being executed with this larger timerange.
            // If an event processor was configured with a processingHopSize greater than the processingWindowSize
            // we can't use the catchup mode.
            final long catchUpSize = configurationProvider.get().eventCatchupWindow();
            if (catchUpSize > 0 && catchUpSize > config.processingWindowSize() && to.plus(catchUpSize).isBefore(now) &&
                config.processingHopSize() <= config.processingWindowSize()) {
                final long chunkCount = catchUpSize / config.processingWindowSize();

                // Align to multiples of the processingWindowSize
                nextTo = to.plus(config.processingWindowSize() * chunkCount);
                LOG.info("Event processor <{}> is catching up on old data. Combining {} search windows with catchUpWindowSize={}ms: from={} to={}",
                        config.eventDefinitionId(), chunkCount, catchUpSize, nextFrom, nextTo);
            }

            LOG.trace("Set new timerange of eventproc <{}> in job trigger data: from={} to={} (hopSize={}ms windowSize={}ms)",
                    config.eventDefinitionId(), nextFrom, nextTo, config.processingHopSize(), config.processingWindowSize());

            final Data newData = data.map(Data::toBuilder).orElse(Data.builder())
                    .timerangeFrom(nextFrom)
                    .timerangeTo(nextTo)
                    .build();

            final Optional<DateTime> nextTime = scheduleStrategies.nextTime(ctx.trigger());

            // The nextTime Optional can be empty if there will be no further executions of the trigger
            if (nextTime.isPresent()) {
                if (nextTo.isBefore(now)) {
                    // If the next "to" timestamp of the timerange to process is in the past, we want to schedule the next
                    // execution of this job as soon as possible to make sure we catch up.
                    LOG.trace("Set nextTime to <{}> to catch up faster - calculated nextTime was <{}>", now, nextTime.get());
                    return JobTriggerUpdate.withNextTimeAndData(now, newData);
                } else if (nextTo.isBefore(nextTime.get())) {
                    LOG.trace("Set nextTime to <{}> because it's closer to the timerange time - calculated nextTime was <{}>", nextTo, nextTime.get());
                    return JobTriggerUpdate.withNextTimeAndData(nextTo, newData);
                } else {
                    // Otherwise use the calculated nextTime
                    LOG.trace("Set nextTime to <{}>", nextTime.get());
                    return JobTriggerUpdate.withNextTimeAndData(nextTime.get(), newData);
                }
            } else {
                // Or no next time if this has been a ONCE trigger
                LOG.trace("No nextTime for trigger <{}>", ctx.trigger().id());
                return JobTriggerUpdate.withoutNextTime();
            }
        } catch (EventProcessorPreconditionException e) {
            // A precondition for the event processor is not ready yet. This job must be retried.
            if (e.getEventDefinition().isPresent()) {
                LOG.debug("Event processor <{}/{}> couldn't be executed because of a failed precondition (retry in {} ms)",
                        e.getEventDefinition().get().title(), e.getEventDefinitionId(), RETRY_INTERVAL);
            } else {
                LOG.debug("Event processor <{}> couldn't be executed because of a failed precondition (retry in {} ms)",
                        e.getEventDefinitionId(), RETRY_INTERVAL);
            }

            return ctx.jobTriggerUpdates().retryIn(RETRY_INTERVAL, MILLISECONDS);
        } catch (EventProcessorException e) {
            if (e.getEventDefinition().isPresent()) {
                LOG.error("Event processor <{}/{}> failed to execute: {} (retry in {} ms)",
                        e.getEventDefinition().get().config().type(), e.getEventDefinitionId(), e.getMessage(), RETRY_INTERVAL, e);
            } else {
                LOG.error("Event processor <{}> failed to execute: {} (retry in {} ms)", e.getEventDefinitionId(), e.getMessage(), RETRY_INTERVAL, e);
            }
            if (e.isPermanent()) {
                // We cannot retry a permanent error so we have to set the job trigger status to ERROR so it doesn't
                // get executed again
                LOG.error("Caught a permanent error, trigger <{}> will go into ERROR state - it will not be executed anymore and needs manual intervention! (event-definition-id: {} job-definition={}/{})",
                        ctx.trigger().id(), e.getEventDefinitionId(), ctx.definition().id(), ctx.definition().title());
                return JobTriggerUpdate.withError(ctx.trigger());
            }

            return ctx.jobTriggerUpdates().retryIn(RETRY_INTERVAL, MILLISECONDS);
        } catch (Exception e) {
            LOG.error("Event processor <{}> failed to execute: parameters={} (retry in {} ms)", config.eventDefinitionId(), parameters, RETRY_INTERVAL, e);

            return ctx.jobTriggerUpdates().retryIn(RETRY_INTERVAL, MILLISECONDS);
        }
    }

    @AutoValue
    @JsonTypeName(EventProcessorExecutionJob.TYPE_NAME)
    @JsonDeserialize(builder = Config.Builder.class)
    public static abstract class Config implements JobDefinitionConfig {
        public static final String FIELD_EVENT_DEFINITION_ID = "event_definition_id";
        private static final String FIELD_PARAMETERS = "parameters";
        private static final String FIELD_PROCESSING_WINDOW_SIZE = "processing_window_size";
        private static final String FIELD_PROCESSING_HOP_SIZE = "processing_hop_size";

        @JsonProperty(FIELD_EVENT_DEFINITION_ID)
        public abstract String eventDefinitionId();

        @JsonProperty(FIELD_PARAMETERS)
        public abstract EventProcessorParametersWithTimerange parameters();

        @JsonProperty(FIELD_PROCESSING_WINDOW_SIZE)
        public abstract long processingWindowSize();

        @JsonProperty(FIELD_PROCESSING_HOP_SIZE)
        public abstract long processingHopSize();

        public static Builder builder() {
            return Builder.create();
        }

        public abstract Builder toBuilder();

        public boolean hasEqualSchedule(Config other) {
            return processingWindowSize() == other.processingWindowSize() &&
                    processingHopSize() == other.processingHopSize();
        }

        @AutoValue.Builder
        public static abstract class Builder implements JobDefinitionConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_EventProcessorExecutionJob_Config.Builder().type(TYPE_NAME);
            }

            @JsonProperty(FIELD_EVENT_DEFINITION_ID)
            public abstract Builder eventDefinitionId(String eventDefinitionId);

            @JsonProperty(FIELD_PARAMETERS)
            public abstract Builder parameters(EventProcessorParametersWithTimerange parameters);

            @JsonProperty(FIELD_PROCESSING_WINDOW_SIZE)
            public abstract Builder processingWindowSize(long windowSize);

            @JsonProperty(FIELD_PROCESSING_HOP_SIZE)
            public abstract Builder processingHopSize(long hopSize);

            abstract Config autoBuild();

            public Config build() {
                // Make sure the type name is correct!
                type(TYPE_NAME);

                return autoBuild();
            }
        }
    }

    @AutoValue
    @JsonTypeName(EventProcessorExecutionJob.TYPE_NAME)
    @JsonDeserialize(builder = Data.Builder.class)
    public static abstract class Data implements JobTriggerData {
        private static final String FIELD_TIMERANGE_FROM = "timerange_from";
        private static final String FIELD_TIMERANGE_TO = "timerange_to";

        @JsonProperty(FIELD_TIMERANGE_FROM)
        public abstract DateTime timerangeFrom();

        @JsonProperty(FIELD_TIMERANGE_TO)
        public abstract DateTime timerangeTo();

        public static Data create(DateTime from, DateTime to) {
            requireNonNull(from, "from cannot be null");
            requireNonNull(to, "to cannot be null");
            checkArgument(from.isBefore(to), "from must be before to");

            return builder().timerangeFrom(from).timerangeTo(to).build();
        }

        public static Builder builder() {
            return Builder.create();
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder implements JobTriggerData.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_EventProcessorExecutionJob_Data.Builder().type(TYPE_NAME);
            }

            @JsonProperty(FIELD_TIMERANGE_FROM)
            public abstract Builder timerangeFrom(DateTime from);

            @JsonProperty(FIELD_TIMERANGE_TO)
            public abstract Builder timerangeTo(DateTime to);

            abstract Data autoBuild();

            public Data build() {
                // Make sure the type name is correct!
                type(TYPE_NAME);

                return autoBuild();
            }
        }
    }
}
