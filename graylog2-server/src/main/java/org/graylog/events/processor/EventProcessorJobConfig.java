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

import org.graylog.scheduler.JobDefinitionConfig;

/**
 * Job definition config of a job that executes an event definition in time windows.
 * <p>
 * {@link EventProcessorExecutionJob.Config} is the default. A plugin can provide its own job type for event
 * definitions by returning another implementation from {@link EventProcessorConfig#toJobSchedulerConfig}. The job
 * definition must keep the {@link EventProcessorExecutionJob.Config#FIELD_EVENT_DEFINITION_ID} field, because
 * job definitions are looked up by it. The job's trigger data must keep the {@code timerange_from} and
 * {@code timerange_to} fields of {@link EventProcessorExecutionJob.Data}, because the UI reads them.
 * <p>
 * A definition can't change its job type on update.
 */
public interface EventProcessorJobConfig extends JobDefinitionConfig {
    String eventDefinitionId();

    EventProcessorParametersWithTimerange parameters();

    long processingWindowSize();

    long processingHopSize();

    boolean isCron();

    boolean disableCatchup();

    default boolean hasEqualSchedule(EventProcessorJobConfig other) {
        return processingWindowSize() == other.processingWindowSize() &&
                processingHopSize() == other.processingHopSize();
    }
}
