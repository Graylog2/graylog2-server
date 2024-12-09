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
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = EventsSearchFilter.Builder.class)
public abstract class EventsSearchFilter {
    private static final String FIELD_ALERTS = "alerts";
    private static final String FIELD_EVENT_DEFINITIONS = "event_definitions";
    private static final String FIELD_PRIORITY = "priority";
    private static final String FIELD_AGGREGATION_TIMERANGE = "aggregation_timerange";
    private static final String FIELD_KEY = "key";

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

    @JsonProperty(FIELD_PRIORITY)
    public abstract Set<String> priority();

    @JsonProperty(FIELD_AGGREGATION_TIMERANGE)
    public abstract Optional<TimeRange> aggregationTimerange();

    @JsonProperty(FIELD_KEY)
    public abstract Set<String> key();

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
                    .eventDefinitions(Collections.emptySet())
                    .priority(Collections.emptySet())
                    .key(Collections.emptySet());
        }

        @JsonProperty(FIELD_ALERTS)
        public abstract Builder alerts(Alerts alerts);

        @JsonProperty(FIELD_EVENT_DEFINITIONS)
        public abstract Builder eventDefinitions(Set<String> eventDefinitions);

        @JsonProperty(FIELD_PRIORITY)
        public abstract Builder priority(Set<String> priority);

        @JsonProperty(FIELD_AGGREGATION_TIMERANGE)
        public abstract Builder aggregationTimerange(TimeRange aggregationTimerange);

        @JsonProperty(FIELD_KEY)
        public abstract Builder key(Set<String> key);

        public abstract EventsSearchFilter build();
    }
}
