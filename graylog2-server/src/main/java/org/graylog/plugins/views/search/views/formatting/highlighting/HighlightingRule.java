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
package org.graylog.plugins.views.search.views.formatting.highlighting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@JsonDeserialize(builder = HighlightingRule.Builder.class)
@WithBeanGetter
public abstract class HighlightingRule {
    static final String FIELD_FIELD = "field";
    static final String FIELD_VALUE = "value";
    static final String FIELD_CONDITION = "condition";
    static final String FIELD_COLOR = "color";

    @JsonProperty(FIELD_FIELD)
    public abstract String field();

    @JsonProperty(FIELD_VALUE)
    public abstract String value();

    @JsonProperty(FIELD_COLOR)
    public abstract String color();


    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_FIELD)
        public abstract Builder field(String field);
        @JsonProperty(FIELD_VALUE)
        public abstract Builder value(String value);
        @JsonProperty(FIELD_COLOR)
        public abstract Builder color(String color);

        public abstract HighlightingRule build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_HighlightingRule.Builder();
        }
    }
}
