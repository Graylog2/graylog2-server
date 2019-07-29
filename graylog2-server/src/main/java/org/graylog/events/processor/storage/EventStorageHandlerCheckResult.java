package org.graylog.events.processor.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = EventStorageHandlerCheckResult.Builder.class)
public abstract class EventStorageHandlerCheckResult {
    public abstract boolean canExecute();

    public abstract Optional<String> message();

    public static EventStorageHandlerCheckResult canExecute(boolean canExecute) {
        return builder().canExecute(canExecute).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventStorageHandlerCheckResult.Builder();
        }

        public abstract Builder canExecute(boolean isExecutable);

        public abstract Builder message(@Nullable String message);

        public abstract EventStorageHandlerCheckResult build();
    }
}