package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = ChartColorSetting.Builder.class)
public abstract class ChartColorSetting {
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

        public abstract ChartColorSetting build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_ChartColorSetting.Builder();
        }
    }
}
