package org.graylog2.storage.versionprobe;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ErrorResponse {
    public abstract Optional<Error> error();

    @JsonCreator
    public static ErrorResponse create(@JsonProperty("error") @Nullable Error error) {
        return new AutoValue_ErrorResponse(Optional.ofNullable(error));
    }

    @Override
    public String toString() {
        return "Error response: " + error().map(Error::toString).orElse("unknown");
    }
}
