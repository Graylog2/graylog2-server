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
public abstract class Error {
    public abstract Optional<String> type();
    public abstract Optional<String> reason();

    @JsonCreator
    public static Error create(@JsonProperty("type") @Nullable String type,
                               @JsonProperty("reason") @Nullable String reason) {
        return new AutoValue_Error(Optional.ofNullable(type), Optional.ofNullable(reason));
    }

    @Override
    public String toString() {
        return "type: " + type().orElse("n/a") + " - reason: " + reason().orElse("n/a");
    }
}
