package org.graylog.events.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.plugin.rest.ValidationResult;

import java.util.Optional;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = EventProcessorConfig.TYPE_FIELD,
        visible = true,
        defaultImpl = EventProcessorConfig.FallbackConfig.class)
public interface EventProcessorConfig {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    /**
     * Returns a {@link JobDefinitionConfig} for this event processor configuration. If the event processor shouldn't
     * be scheduled, this method returns an empty {@link Optional}.
     *
     * @param eventDefinition the event definition
     * @param clock           the clock that can be used to get the current time
     * @return the job definition config or an empty optional if the processor shouldn't be scheduled
     */
    @JsonIgnore
    default Optional<EventProcessorSchedulerConfig> toJobSchedulerConfig(EventDefinition eventDefinition, JobSchedulerClock clock) {
        return Optional.empty();
    }

    @JsonIgnore
    ValidationResult validate();

    interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }

    class FallbackConfig implements EventProcessorConfig {
        @Override
        public String type() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ValidationResult validate() {
            throw new UnsupportedOperationException();
        }
    }
}
