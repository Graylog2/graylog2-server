package org.graylog2.plugin.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
@JsonTypeName("RequestError") // Explicitly indicates the class type to avoid AutoValue_ at the beginning
public abstract class RequestError implements GenericError {
    @JsonProperty
    public abstract int line();

    @JsonProperty
    public abstract int column();

    @JsonProperty
    public abstract String path();

    public static RequestError create(String message, int line, int column, String path) {
        return new AutoValue_RequestError(message, line, column, path);
    }

}
