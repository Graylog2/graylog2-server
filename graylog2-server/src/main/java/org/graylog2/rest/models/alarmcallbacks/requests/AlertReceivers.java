package org.graylog2.rest.models.alarmcallbacks.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class AlertReceivers {
    @JsonProperty("emails")
    public abstract List<String> emails();

    @JsonProperty("users")
    public abstract List<String> users();

    @JsonCreator
    public static AlertReceivers create(@JsonProperty("emails") List<String> emails,
                                        @JsonProperty("users") List<String> users) {
        return new AutoValue_AlertReceivers(emails, users);
    }
}
