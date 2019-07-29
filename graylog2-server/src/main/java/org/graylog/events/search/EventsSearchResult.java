package org.graylog.events.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog.events.event.EventDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

@AutoValue
public abstract class EventsSearchResult {
    @JsonProperty("events")
    public abstract List<Event> events();

    @JsonProperty("used_indices")
    public abstract Set<String> usedIndices();

    @JsonProperty("parameters")
    public abstract EventsSearchParameters parameters();

    @JsonProperty("total_events")
    public abstract long totalEvents();

    @JsonProperty("duration")
    public abstract long duration();

    @JsonProperty("context")
    public abstract Context context();

    public static Builder builder() {
        return new AutoValue_EventsSearchResult.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder events(List<Event> events);

        public abstract Builder usedIndices(Set<String> usedIndices);

        public abstract Builder parameters(EventsSearchParameters parameters);

        public abstract Builder totalEvents(long totalEvents);

        public abstract Builder duration(long duration);

        public abstract Builder context(Context context);

        public abstract EventsSearchResult build();
    }

    @AutoValue
    public static abstract class Event {
        @JsonProperty("event")
        public abstract EventDto event();

        @JsonProperty("index_name")
        public abstract String indexName();

        @JsonProperty("index_type")
        public abstract String indexType();

        public static Event create(EventDto event, String indexName, String indexType) {
            return new AutoValue_EventsSearchResult_Event(event, indexName, indexType);
        }
    }

    @AutoValue
    public static abstract class Context {
        @JsonProperty("event_definitions")
        public abstract ImmutableMap<String, ContextEntity> eventDefinitions();

        @JsonProperty("streams")
        public abstract ImmutableMap<String, ContextEntity> streams();

        public static Context create(Map<String, ContextEntity> eventDefinitions,
                                     Map<String, ContextEntity> streams) {
            return new AutoValue_EventsSearchResult_Context(ImmutableMap.copyOf(eventDefinitions), ImmutableMap.copyOf(streams));
        }
    }

    @AutoValue
    public static abstract class ContextEntity {
        @JsonProperty("id")
        public abstract String id();

        @JsonProperty("title")
        public abstract String title();

        @JsonProperty("description")
        public abstract String description();

        public static ContextEntity create(String id, String title, String description) {
            return new AutoValue_EventsSearchResult_ContextEntity(id, title, description);
        }
    }
}