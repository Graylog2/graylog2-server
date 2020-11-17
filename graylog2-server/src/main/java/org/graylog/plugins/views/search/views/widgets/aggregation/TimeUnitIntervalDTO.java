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
package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Locale;

@AutoValue
@JsonTypeName(TimeUnitIntervalDTO.type)
@JsonDeserialize(builder = TimeUnitIntervalDTO.Builder.class)
public abstract class TimeUnitIntervalDTO implements IntervalDTO {
    public static final String type = "timeunit";

    enum IntervalUnit {
        SECONDS("seconds"),
        MINUTES("minutes"),
        HOURS("hours"),
        DAYS("days"),
        WEEKS("weeks"),
        MONTHS("months"),
        YEARS("years"),

        AUTO("auto");

        private final String name;

        IntervalUnit(String name) {
            this.name = name;
        }

        @Override
        @JsonValue
        public String toString() {
            return this.name;
        }
    }

    static final String FIELD_TYPE = "type";
    static final String FIELD_VALUE = "value";
    static final String FIELD_UNIT = "unit";

    @JsonProperty
    public abstract String type();

    @JsonProperty(FIELD_VALUE)
    public abstract int value();

    @JsonProperty(FIELD_UNIT)
    public abstract IntervalUnit unit();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        @JsonProperty(FIELD_VALUE)
        public abstract Builder value(int value);

        public abstract Builder unit(IntervalUnit unit);

        @JsonProperty(FIELD_UNIT)
        public Builder unit(String unit) {
            return unit(IntervalUnit.valueOf(unit.toUpperCase(Locale.ENGLISH)));
        }

        public abstract TimeUnitIntervalDTO build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_TimeUnitIntervalDTO.Builder().type(type);
        }
    }
}
