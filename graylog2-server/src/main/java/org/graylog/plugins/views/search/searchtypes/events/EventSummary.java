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
import com.google.common.collect.ImmutableSet;
import org.graylog.events.event.EventDto;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = EventSummary.Builder.class)
public abstract class EventSummary implements CommonEventSummary {
    @SuppressWarnings("unchecked")
    public static EventSummary parse(Map<String, Object> rawEvent) {
        return EventSummary.builder()
                .alert((boolean) rawEvent.get(EventDto.FIELD_ALERT))
                .id((String) rawEvent.get(EventDto.FIELD_ID))
                .message((String) rawEvent.get(EventDto.FIELD_MESSAGE))
                .streams(ImmutableSet.copyOf((List<String>) rawEvent.get(EventDto.FIELD_SOURCE_STREAMS)))
                .timestamp(DateTime.parse((String) rawEvent.get(EventDto.FIELD_EVENT_TIMESTAMP), Tools.ES_DATE_FORMAT_FORMATTER))
                .eventDefinitionId((String) rawEvent.get(EventDto.FIELD_EVENT_DEFINITION_ID))
                .priority((Integer) rawEvent.get(EventDto.FIELD_PRIORITY))
                .eventKeys(List.copyOf((List<String>) rawEvent.get(EventDto.FIELD_KEY_TUPLE)))
                .rawEvent(rawEvent)
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
            return new AutoValue_EventSummary.Builder()
                    .rawEvent(Map.of());
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
        public abstract Builder priority(Integer priority);

        @JsonProperty(FIELD_EVENT_KEYS)
        public abstract Builder eventKeys(List<String> eventKeys);

        @JsonIgnore
        public abstract Builder rawEvent(Map<String, Object> rawEvent);

        public abstract EventSummary build();
    }
}
