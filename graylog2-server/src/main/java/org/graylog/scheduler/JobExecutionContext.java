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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.concurrent.atomic.AtomicBoolean;

@AutoValue
@JsonDeserialize(builder = JobExecutionContext.Builder.class)
public abstract class JobExecutionContext {
    public abstract JobTriggerDto trigger();

    public abstract JobDefinitionDto definition();

    public abstract JobTriggerUpdates jobTriggerUpdates();

    public abstract AtomicBoolean isRunning();

    public static JobExecutionContext create(JobTriggerDto trigger, JobDefinitionDto definition, JobTriggerUpdates jobTriggerUpdates, AtomicBoolean isRunning) {
        return builder()
                .trigger(trigger)
                .definition(definition)
                .jobTriggerUpdates(jobTriggerUpdates)
                .isRunning(isRunning)
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_JobExecutionContext.Builder();
        }

        public abstract Builder trigger(JobTriggerDto trigger);

        public abstract Builder definition(JobDefinitionDto definition);

        public abstract Builder jobTriggerUpdates(JobTriggerUpdates jobTriggerUpdates);

        public abstract Builder isRunning(AtomicBoolean isRunning);

        public abstract JobExecutionContext build();
    }
}
