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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = DashboardWidgetDTO.Builder.class)
public abstract class DashboardWidgetDTO {
    private static final String FIELD_QUERY_ID = "query_id";
    private static final String FIELD_WIDGET_ID = "widget_id";

    @JsonProperty(FIELD_QUERY_ID)
    public abstract String queryId();

    @JsonProperty(FIELD_WIDGET_ID)
    public abstract String widgetID();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_DashboardWidgetDTO.Builder();
        }

        @JsonProperty(FIELD_QUERY_ID)
        public abstract Builder queryId(String queryId);

        @JsonProperty(FIELD_WIDGET_ID)
        public abstract Builder widgetID(String widgetId);

        public abstract DashboardWidgetDTO build();
    }
}
