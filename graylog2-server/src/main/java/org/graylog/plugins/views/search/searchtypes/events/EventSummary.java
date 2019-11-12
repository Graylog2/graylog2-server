package org.graylog.plugins.views.search.searchtypes.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.event.EventDto;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = EventSummary.Builder.class)
public abstract class EventSummary {
    private static final String FIELD_ID = "id";
    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_EVENT_TIMESTAMP = "timestamp";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_ALERT = "alert";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @JsonProperty(FIELD_EVENT_TIMESTAMP)
    public abstract DateTime timestamp();

    @JsonProperty(FIELD_MESSAGE)
    public abstract String message();

    @JsonProperty(FIELD_ALERT)
    public abstract boolean alert();

    @SuppressWarnings("unchecked")
    public static EventSummary parse(Map<String, Object> rawEvent) {
        return EventSummary.builder()
                .alert((boolean) rawEvent.get(EventDto.FIELD_ALERT))
                .id((String) rawEvent.get(EventDto.FIELD_ID))
                .message((String) rawEvent.get(EventDto.FIELD_MESSAGE))
                .streams(ImmutableSet.copyOf((ArrayList<String>) rawEvent.get(EventDto.FIELD_SOURCE_STREAMS)))
                .timestamp(DateTime.parse((String) rawEvent.get(EventDto.FIELD_EVENT_TIMESTAMP), Tools.ES_DATE_FORMAT_FORMATTER))
                .build();
    }

    public static EventSummary.Builder builder() {
        return EventSummary.Builder.create();
    }

    public abstract EventSummary.Builder toBuilder();

    @AutoValue.Builder
    @JsonIgnoreProperties({"_id"})
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventSummary.Builder();
        }

        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_EVENT_TIMESTAMP)
        public abstract Builder timestamp(DateTime timestamp);

        @JsonProperty(FIELD_MESSAGE)
        public abstract Builder message(String message);

        @JsonProperty(FIELD_ALERT)
        public abstract Builder alert(boolean alert);

        public abstract EventSummary build();
    }
}
