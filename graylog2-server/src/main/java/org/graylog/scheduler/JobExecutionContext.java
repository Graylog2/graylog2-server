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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoValue
@JsonDeserialize(builder = JobExecutionContext.Builder.class)
public abstract class JobExecutionContext {
    public abstract JobTriggerDto trigger();

    public abstract JobDefinitionDto definition();

    public abstract JobTriggerUpdates jobTriggerUpdates();

    public abstract AtomicBoolean schedulerIsRunning();

    public boolean isCancelled() {
        if (!schedulerIsRunning().get()) {
            return true;
        }
        final Optional<JobTriggerDto> triggerDto = jobTriggerService().get(trigger().id());
        return triggerDto.map(JobTriggerDto::isCancelled).orElse(false);
    }

    abstract DBJobTriggerService jobTriggerService();
    public void updateProgress(int progress) {
        jobTriggerService().updateProgress(trigger(), progress);
    }

    public static JobExecutionContext create(JobTriggerDto trigger, JobDefinitionDto definition, JobTriggerUpdates jobTriggerUpdates, AtomicBoolean schedulerIsRunning, DBJobTriggerService jobTriggerService) {
        return builder()
                .trigger(trigger)
                .definition(definition)
                .jobTriggerUpdates(jobTriggerUpdates)
                .schedulerIsRunning(schedulerIsRunning)
                .jobTriggerService(jobTriggerService)
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

        public abstract Builder schedulerIsRunning(AtomicBoolean isRunning);

        public abstract Builder jobTriggerService(DBJobTriggerService jobTriggerService);

        public abstract JobExecutionContext build();
    }
}
