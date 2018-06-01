package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class CollectorStatus {
    @JsonProperty("status")
    public abstract int status();

    @JsonProperty("message")
    public abstract String message();

    @JsonCreator
    public static CollectorStatus create(@JsonProperty("status") int status,
                                         @JsonProperty("message") String message) {
        return new AutoValue_CollectorStatus(status, message);
    }
}