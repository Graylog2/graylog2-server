package org.graylog2.rest.models.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class DisplayGettingStarted {

    @JsonProperty("show")
    public abstract boolean show();

    @JsonCreator
    public static DisplayGettingStarted create(@JsonProperty("show") boolean show) {
        return new AutoValue_DisplayGettingStarted(show);
    }
}
