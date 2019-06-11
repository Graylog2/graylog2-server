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
