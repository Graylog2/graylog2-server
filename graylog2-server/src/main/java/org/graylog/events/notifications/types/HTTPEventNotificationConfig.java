package org.graylog.events.notifications.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.scheduler.JobTriggerData;

@AutoValue
@JsonTypeName(HTTPEventNotificationConfig.TYPE_NAME)
@JsonDeserialize(builder = HTTPEventNotificationConfig.Builder.class)
public abstract class HTTPEventNotificationConfig implements EventNotificationConfig {
    public static final String TYPE_NAME = "http-notification-v1";

    private static final String FIELD_URL = "url";

    @JsonProperty(FIELD_URL)
    public abstract String url();

    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_HTTPEventNotificationConfig.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(FIELD_URL)
        public abstract Builder url(String url);

        public abstract HTTPEventNotificationConfig build();
    }
}
