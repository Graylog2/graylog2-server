/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.search.searchtypes.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.event.EventDto;
import org.graylog.events.event.EventReplayInfo;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = EventSummary.Builder.class)
public abstract class EventSummary implements CommonEventSummary {
    @SuppressWarnings("unchecked")
    public static EventSummary parse(EventDto event) {
        return EventSummary.builder()
                .alert(event.alert())
                .id(event.id())
                .message(event.message())
                .streams(event.sourceStreams())
                .timestamp(event.eventTimestamp())
                .eventDefinitionId(event.eventDefinitionId())
                .priority(event.priority())
                .eventKeys(event.keyTuple())
                .replayInfo(event.replayInfo())
                .rawEvent(event)
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

        @JsonProperty(FIELD_EVENT_DEFINITION_ID)
        public abstract Builder eventDefinitionId(String eventDefinitionId);

        @JsonProperty(FIELD_PRIORITY)
        public abstract Builder priority(Long priority);

        @JsonProperty(FIELD_EVENT_KEYS)
        public abstract Builder eventKeys(List<String> eventKeys);

        @JsonProperty(FIELD_REPLAY_INFO)
        public abstract Builder replayInfo(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<EventReplayInfo> replayInfo);

        @JsonIgnore
        public abstract Builder rawEvent(@Nullable EventDto rawEvent);

        public abstract EventSummary build();
    }
}
