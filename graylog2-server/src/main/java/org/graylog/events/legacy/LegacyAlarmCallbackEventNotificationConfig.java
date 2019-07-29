package org.graylog.events.legacy;

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

import java.util.Map;

@AutoValue
@JsonTypeName(LegacyAlarmCallbackEventNotificationConfig.TYPE_NAME)
@JsonDeserialize(builder = LegacyAlarmCallbackEventNotificationConfig.Builder.class)
public abstract class LegacyAlarmCallbackEventNotificationConfig implements EventNotificationConfig {
    public static final String TYPE_NAME = "legacy-alarm-callback-notification-v1";

    private static final String FIELD_CALLBACK_TYPE = "callback_type";
    private static final String FIELD_CONFIGURATION = "configuration";

    @JsonProperty(FIELD_CALLBACK_TYPE)
    public abstract String callbackType();

    @JsonProperty(FIELD_CONFIGURATION)
    public abstract Map<String, Object> configuration();

    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_LegacyAlarmCallbackEventNotificationConfig.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(FIELD_CALLBACK_TYPE)
        public abstract Builder callbackType(String callbackType);

        @JsonProperty(FIELD_CONFIGURATION)
        public abstract Builder configuration(Map<String, Object> configuration);

        public abstract LegacyAlarmCallbackEventNotificationConfig build();
    }
}
