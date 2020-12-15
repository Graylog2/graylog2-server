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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

@AutoValue
@JsonTypeName(value = AbsoluteRange.ABSOLUTE)
public abstract class AbsoluteRange extends TimeRange {

    static final String ABSOLUTE = "absolute";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    public abstract DateTime from();

    @JsonProperty
    public abstract DateTime to();

    static Builder builder() {
        return new AutoValue_AbsoluteRange.Builder();
    }

    @JsonCreator
    static AbsoluteRange create(@JsonProperty("type") String type,
                                @JsonProperty("from") DateTime from,
                                @JsonProperty("to") DateTime to) {
        return builder().type(type).from(from).to(to).build();
    }

    public static AbsoluteRange create(DateTime from, DateTime to) {
        return builder().type(ABSOLUTE).from(from).to(to).build();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract AbsoluteRange build();

        abstract Builder type(String type);

        abstract Builder to(DateTime to);

        abstract Builder from(DateTime to);
    }
}
