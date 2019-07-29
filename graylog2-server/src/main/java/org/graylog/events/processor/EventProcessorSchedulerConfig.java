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
