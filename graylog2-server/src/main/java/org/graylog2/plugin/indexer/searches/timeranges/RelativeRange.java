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
package org.graylog2.plugin.indexer.searches.timeranges;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;

import java.util.Map;

@AutoValue
@JsonTypeName(RelativeRange.RELATIVE)
public abstract class RelativeRange extends TimeRange {

    public static final String RELATIVE = "relative";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    public abstract int range();

    public int getRange() {
        return range();
    }

    @Override
    @JsonIgnore
    public DateTime getFrom() {
        // TODO this should be computed once
        if (range() > 0) {
            return Tools.nowUTC().minus(Seconds.seconds(range()));
        }
        return new DateTime(0, DateTimeZone.UTC);
    }

    @Override
    @JsonIgnore
    public DateTime getTo() {
        // TODO this should be fixed
        return Tools.nowUTC();
    }

    @JsonCreator
    public static RelativeRange create(@JsonProperty("type") String type, @JsonProperty("range") int range) throws InvalidRangeParametersException {
        return builder().type(type).checkRange(range).build();
    }

    public static RelativeRange create(int range) throws InvalidRangeParametersException {
        return create(RELATIVE, range);
    }

    public static Builder builder() {
        return new AutoValue_RelativeRange.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract RelativeRange build();

        public abstract Builder type(String type);

        public abstract Builder range(int range);

        // TODO replace with custom build()
        public Builder checkRange(int range) throws InvalidRangeParametersException {
            if (range < 0) {
                throw new InvalidRangeParametersException("Range must not be negative");
            }
            return range(range);
        }
    }

}
