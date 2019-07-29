package org.graylog.events.notifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.scheduler.JobTriggerDto;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
public abstract class EventNotificationContext {
    public abstract String notificationId();

    public abstract EventNotificationConfig notificationConfig();

    public abstract EventDto event();

    public abstract Optional<EventDefinitionDto> eventDefinition();

    public abstract JobTriggerDto jobTrigger();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventNotificationContext.Builder();
        }

        public abstract Builder notificationId(String notificationId);

        public abstract Builder notificationConfig(EventNotificationConfig notificationConfig);

        public abstract Builder event(EventDto event);

        public abstract Builder eventDefinition(@Nullable EventDefinitionDto eventDefinition);

        public abstract Builder jobTrigger(JobTriggerDto jobTrigger);

        public abstract EventNotificationContext build();
    }
}
