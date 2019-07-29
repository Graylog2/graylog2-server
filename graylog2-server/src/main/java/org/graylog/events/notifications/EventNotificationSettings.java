package org.graylog.events.notifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = EventNotificationSettings.Builder.class)
public abstract class EventNotificationSettings {
    public static final String FIELD_GRACE_PERIOD_MS = "grace_period_ms";
    public static final String FIELD_BACKLOG_SIZE = "backlog_size";

    @JsonProperty(FIELD_GRACE_PERIOD_MS)
    public abstract long gracePeriodMs();

    @JsonProperty(FIELD_BACKLOG_SIZE)
    public abstract long backlogSize();

    public static EventNotificationSettings withGracePeriod(long gracePeriodMs) {
        return builder().gracePeriodMs(gracePeriodMs).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventNotificationSettings.Builder().backlogSize(0L);
        }

        @JsonProperty(FIELD_GRACE_PERIOD_MS)
        public abstract Builder gracePeriodMs(long gracePeriodMs);

        @JsonProperty(FIELD_BACKLOG_SIZE)
        public abstract Builder backlogSize(long backlogSize);

        public abstract EventNotificationSettings build();
    }
}