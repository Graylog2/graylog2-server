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

@AutoValue
@JsonDeserialize(builder = ChartColorMapping.Builder.class)
public abstract class ChartColorMapping {
   private static final String FIELD_NAME = "field_name";
    private static final String FIELD_CHART_COLOR = "chart_color";

    @JsonProperty(FIELD_NAME)
    public abstract String fieldName();

    @JsonProperty(FIELD_CHART_COLOR)
    public abstract ChartColor chartColor();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_NAME)
        public abstract Builder fieldName(String widgetId);

        @JsonProperty(FIELD_CHART_COLOR)
        public abstract Builder chartColor(ChartColor chartColor);

        public abstract ChartColorMapping build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_ChartColorMapping.Builder();
        }
    }
}
