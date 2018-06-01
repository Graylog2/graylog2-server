package org.graylog.plugins.sidecar.rest.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class ValidationResponse {
    @JsonProperty
    public abstract boolean error();

    @JsonProperty
    @Nullable
    public abstract String errorMessage();

    @JsonCreator
    public static ValidationResponse create(@JsonProperty("error") boolean error,
                                            @JsonProperty("error_message") @Nullable String errorMessage) {
        return new AutoValue_ValidationResponse(error, errorMessage);
    }
}
