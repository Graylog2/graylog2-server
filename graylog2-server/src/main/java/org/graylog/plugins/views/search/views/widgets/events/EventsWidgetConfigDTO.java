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
package org.graylog.plugins.views.search.views.widgets.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.views.WidgetConfigDTO;

import java.util.List;
import java.util.Set;

@AutoValue
@JsonTypeName(EventsWidgetConfigDTO.NAME)
@JsonDeserialize(builder = EventsWidgetConfigDTO.Builder.class)
public abstract class EventsWidgetConfigDTO implements WidgetConfigDTO {
    public static final String NAME = "events";

    public enum Direction {
        Ascending,
        Descending;
    }

    public record SortConfig(@JsonProperty("field") String field, @JsonProperty("direction") Direction direction) {
        @JsonCreator
        public static SortConfig create(@JsonProperty("field") String field, @JsonProperty("direction") Direction direction) {
            return new SortConfig(field, direction);
        }
    }

    public enum Mode {
        List,
        Number;
    }

    private static final String FIELD_MODE = "mode";
    private static final String FIELD_FIELDS = "fields";
    private static final String FIELD_FILTERS = "filters";
    private static final String FIELD_SORT = "sort";

    @JsonProperty(FIELD_MODE)
    public abstract Mode mode();

    @JsonProperty(FIELD_FIELDS)
    public abstract Set<String> fields();

    @JsonProperty(FIELD_FILTERS)
    public abstract List<Filter> filters();

    @JsonProperty(FIELD_SORT)
    public abstract SortConfig sort();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_MODE)
        public abstract Builder mode(Mode mode);

        @JsonProperty(FIELD_FIELDS)
        public abstract Builder fields(Set<String> fields);

        @JsonProperty(FIELD_FILTERS)
        public abstract Builder filters(List<Filter> filters);

        @JsonProperty(FIELD_SORT)
        public abstract Builder sort(SortConfig sort);

        public abstract EventsWidgetConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_EventsWidgetConfigDTO.Builder()
                    .mode(Mode.List)
                    .fields(Set.of())
                    .filters(List.of());
        }
    }
}
