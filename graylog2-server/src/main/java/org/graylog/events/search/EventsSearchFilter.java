/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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