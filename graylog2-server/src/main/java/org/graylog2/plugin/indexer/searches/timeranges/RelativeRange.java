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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.OptionalInt;

@AutoValue
@JsonTypeName(RelativeRange.RELATIVE)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonDeserialize(builder = RelativeRange.Builder.class)
public abstract class RelativeRange extends TimeRange {

    public static final String RELATIVE = "relative";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    public abstract OptionalInt range();

    @JsonProperty
    public abstract OptionalInt from();

    @JsonProperty
    public abstract OptionalInt to();

    public int getRange() {
        return range().orElse(0);
    }

    @Override
    @JsonIgnore
    public DateTime getFrom() {
        // TODO this should be computed once
        if (range().isPresent()) {
            return Tools.nowUTC().minusSeconds(range().getAsInt());
        }

        return Tools.nowUTC().minusSeconds(from().orElseThrow(() -> new IllegalStateException("Neither `range` nor `from` specified!")));
    }

    @Override
    @JsonIgnore
    public DateTime getTo() {
        // TODO this should be fixed
        if (range().isPresent()) {
            return Tools.nowUTC();
        }

        return Tools.nowUTC().minusSeconds(to().orElseThrow(() -> new IllegalStateException("Neither `range` nor `to` specified!")));
    }

    @JsonIgnore
    public boolean isAllMessages() {
        return range().orElse(-1) == 0;
    }

    public static RelativeRange create(int range) throws InvalidRangeParametersException {
        return Builder.builder()
                .range(range)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        abstract RelativeRange autoBuild();

        @JsonProperty("type")
        public abstract Builder type(String type);

        @JsonProperty("range")
        public abstract Builder range(int range);
        abstract OptionalInt range();

        @JsonProperty("from")
        public abstract Builder from(int from);
        abstract OptionalInt from();

        @JsonProperty("to")
        public abstract Builder to(int to);
        abstract OptionalInt to();

        public RelativeRange build() throws InvalidRangeParametersException {
            if (range().isPresent() && (from().isPresent() || to().isPresent())) {
                throw new InvalidRangeParametersException("Either `range` OR `from`/`to` must be specifed, not both!");
            }

            if (range().isPresent()) {
                if (range().getAsInt() < 0) {
                    throw new InvalidRangeParametersException("Range must not be negative");
                }
            }
            return autoBuild();
        }

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_RelativeRange.Builder().type(RELATIVE);
        }
    }

}
