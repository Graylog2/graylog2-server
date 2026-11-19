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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.scheduler.JobDefinitionConfig;

/**
 * Job definition config of a job that executes an event definition in time windows.
 * <p>
 * {@link EventProcessorExecutionJob.Config} is the default. A plugin can provide its own job type for event
 * definitions by returning another implementation from {@link EventProcessorConfig#toJobSchedulerConfig}. The
 * annotations here fix the field names; job definitions are looked up by {@link #FIELD_EVENT_DEFINITION_ID}. An
 * AutoValue implementation inherits the getters, and its builder implements {@link Builder}. The job's trigger data must keep the {@code timerange_from} and
 * {@code timerange_to} fields of {@link EventProcessorExecutionJob.Data}, because the UI reads them.
 * <p>
 * An update that changes the job type replaces the trigger; the new job starts at the current time with empty data.
 */
public interface EventProcessorJobConfig extends JobDefinitionConfig {
    String FIELD_EVENT_DEFINITION_ID = "event_definition_id";
    String FIELD_PARAMETERS = "parameters";
    String FIELD_PROCESSING_WINDOW_SIZE = "processing_window_size";
    String FIELD_PROCESSING_HOP_SIZE = "processing_hop_size";
    String FIELD_IS_CRON = "is_cron";
    String FIELD_DISABLE_CATCHUP = "disable_catchup";

    @JsonProperty(FIELD_EVENT_DEFINITION_ID)
    String eventDefinitionId();

    @JsonProperty(FIELD_PARAMETERS)
    EventProcessorParametersWithTimerange parameters();

    @JsonProperty(FIELD_PROCESSING_WINDOW_SIZE)
    long processingWindowSize();

    @JsonProperty(FIELD_PROCESSING_HOP_SIZE)
    long processingHopSize();

    @JsonProperty(FIELD_IS_CRON)
    boolean isCron();

    @JsonProperty(FIELD_DISABLE_CATCHUP)
    boolean disableCatchup();

    default boolean hasEqualSchedule(EventProcessorJobConfig other) {
        return processingWindowSize() == other.processingWindowSize() &&
                processingHopSize() == other.processingHopSize();
    }

    interface Builder<SELF> extends JobDefinitionConfig.Builder<SELF> {
        @JsonProperty(FIELD_EVENT_DEFINITION_ID)
        SELF eventDefinitionId(String eventDefinitionId);

        @JsonProperty(FIELD_PARAMETERS)
        SELF parameters(EventProcessorParametersWithTimerange parameters);

        @JsonProperty(FIELD_PROCESSING_WINDOW_SIZE)
        SELF processingWindowSize(long windowSize);

        @JsonProperty(FIELD_PROCESSING_HOP_SIZE)
        SELF processingHopSize(long hopSize);

        @JsonProperty(FIELD_IS_CRON)
        SELF isCron(boolean isCron);

        @JsonProperty(value = FIELD_DISABLE_CATCHUP, defaultValue = "false")
        SELF disableCatchup(boolean disableCatchup);
    }
}
