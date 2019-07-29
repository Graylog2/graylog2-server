package org.graylog.events.notifications;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog.events.event.EventDto;
import org.graylog.scheduler.JobTriggerData;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = EventNotificationConfig.TYPE_FIELD,
        visible = true,
        defaultImpl = EventNotificationConfig.FallbackNotificationConfig.class)
public interface EventNotificationConfig {
    String FIELD_NOTIFICATION_ID = "notification_id";
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }

    @JsonIgnore
    JobTriggerData toJobTriggerData(EventDto dto);

    class FallbackNotificationConfig implements EventNotificationConfig {
        @Override
        public String type() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JobTriggerData toJobTriggerData(EventDto dto) {
            return null;
        }
    }
}
