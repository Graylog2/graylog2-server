/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
