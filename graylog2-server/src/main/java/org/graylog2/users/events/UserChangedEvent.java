package org.graylog2.users.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class UserChangedEvent {
    private static final String FIELD_USER_ID = "user_id";

    @JsonProperty(FIELD_USER_ID)
    public abstract String userId();

    @JsonCreator
    public static UserChangedEvent create(@JsonProperty(FIELD_USER_ID) String userId) {
        return new AutoValue_UserChangedEvent(userId);
    }
}
