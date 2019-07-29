package org.graylog.events.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = NotificationParameters.TYPE_FIELD,
        visible = true,
        defaultImpl = NotificationParameters.FallbackParameters.class)
public interface NotificationParameters {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }

    class FallbackParameters implements NotificationParameters {
        @Override
        public String type() {
            return "";
        }
    }
}
