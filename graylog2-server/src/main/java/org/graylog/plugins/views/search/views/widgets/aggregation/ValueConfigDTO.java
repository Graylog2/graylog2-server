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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.OptionalInt;

@AutoValue
@JsonTypeName(ValueConfigDTO.NAME)
@JsonDeserialize(builder = ValueConfigDTO.Builder.class)
public abstract class ValueConfigDTO implements PivotConfigDTO {
    public static final String NAME = "values";
    static final String FIELD_LIMIT = "limit";

    @JsonProperty
    public abstract OptionalInt limit();

    public static ValueConfigDTO create() {
        return Builder.builder().build();
    }

    abstract Builder toBuilder();

    public ValueConfigDTO withLimit(int limit) {
        return toBuilder().limit(limit).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_LIMIT)
        public abstract Builder limit(int limit);

        public abstract ValueConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_ValueConfigDTO.Builder();
        }
    }
}
