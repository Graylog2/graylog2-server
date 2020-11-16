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

import com.google.auto.value.AutoValue;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.JobSchedule;

@AutoValue
public abstract class EventProcessorSchedulerConfig {
    public abstract JobDefinitionConfig jobDefinitionConfig();

    public abstract JobSchedule schedule();

    public static Builder builder() {
        return new AutoValue_EventProcessorSchedulerConfig.Builder();
    }

    public static EventProcessorSchedulerConfig create(JobDefinitionConfig jobDefinitionConfig, JobSchedule schedule) {
        return builder().jobDefinitionConfig(jobDefinitionConfig).schedule(schedule).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder jobDefinitionConfig(JobDefinitionConfig jobDefinitionConfig);

        public abstract Builder schedule(JobSchedule jobSchedule);

        public abstract EventProcessorSchedulerConfig build();
    }
}
