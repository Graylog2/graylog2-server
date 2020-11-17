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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = SeriesConfigDTO.Builder.class)
@WithBeanGetter
public abstract class SeriesConfigDTO {
    static final String FIELD_NAME = "name";

    public static SeriesConfigDTO empty() {
        return Builder.builder().build();
    }

    @JsonProperty(FIELD_NAME)
    @Nullable
    public abstract String name();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_NAME)
        @Nullable
        public abstract Builder name(String name);

        public abstract SeriesConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_SeriesConfigDTO.Builder();
        }
    }
}
