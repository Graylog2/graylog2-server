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
package org.graylog.events.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = EventsSearchFilter.Builder.class)
public abstract class EventsSearchFilter {
    private static final String FIELD_ALERTS = "alerts";
    private static final String FIELD_EVENT_DEFINITIONS = "event_definitions";

    public enum Alerts {
        @JsonProperty("include")
        INCLUDE,
        @JsonProperty("exclude")
        EXCLUDE,
        @JsonProperty("only")
        ONLY
    }

    @JsonProperty(FIELD_ALERTS)
    public abstract Alerts alerts();

    @JsonProperty(FIELD_EVENT_DEFINITIONS)
    public abstract Set<String> eventDefinitions();

    public static EventsSearchFilter empty() {
        return builder().build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventsSearchFilter.Builder()
                    .alerts(Alerts.INCLUDE)
                    .eventDefinitions(Collections.emptySet());
        }

        @JsonProperty(FIELD_ALERTS)
        public abstract Builder alerts(Alerts alerts);

        @JsonProperty(FIELD_EVENT_DEFINITIONS)
        public abstract Builder eventDefinitions(Set<String> eventDefinitions);

        public abstract EventsSearchFilter build();
    }
}