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
