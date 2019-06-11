package org.graylog.plugins.enterprise.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.views.WidgetConfigDTO;
import org.graylog.plugins.enterprise.search.views.widgets.aggregation.sort.SortConfigDTO;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
@JsonTypeName(AggregationConfigDTO.NAME)
@JsonDeserialize(builder = AggregationConfigDTO.Builder.class)
public abstract class AggregationConfigDTO implements WidgetConfigDTO {
    public static final String NAME = "aggregation";
    static final String FIELD_ROW_PIVOTS = "row_pivots";
    static final String FIELD_COLUMN_PIVOTS = "column_pivots";
    static final String FIELD_SERIES = "series";
    static final String FIELD_SORT = "sort";
    static final String FIELD_VISUALIZATION = "visualization";
    static final String FIELD_VISUALIZATION_CONFIG = "visualization_config";
    static final String FIELD_ROLLUP = "rollup";
    static final String FIELD_FORMATTING_SETTINGS = "formatting_settings";

    @JsonProperty(FIELD_ROW_PIVOTS)
    public abstract List<PivotDTO> rowPivots();

    @JsonProperty(FIELD_COLUMN_PIVOTS)
    public abstract List<PivotDTO> columnPivots();

    @JsonProperty(FIELD_SERIES)
    public abstract List<SeriesDTO> series();

    @JsonProperty(FIELD_SORT)
    public abstract List<SortConfigDTO> sort();

    @JsonProperty(FIELD_VISUALIZATION)
    public abstract String visualization();

    @JsonProperty(FIELD_VISUALIZATION_CONFIG)
    @Nullable
    public abstract VisualizationConfigDTO visualizationConfig();

    @JsonProperty(FIELD_FORMATTING_SETTINGS)
    @Nullable
    public abstract WidgetFormattingSettings formattingSettings();

    @JsonProperty(FIELD_ROLLUP)
    public abstract boolean rollup();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_ROW_PIVOTS)
        public abstract Builder rowPivots(List<PivotDTO> rowPivots);

        @JsonProperty(FIELD_COLUMN_PIVOTS)
        public abstract Builder columnPivots(List<PivotDTO> columnPivots);

        @JsonProperty(FIELD_SERIES)
        public abstract Builder series(List<SeriesDTO> series);

        @JsonProperty(FIELD_SORT)
        public abstract Builder sort(List<SortConfigDTO> sort);

        @JsonProperty(FIELD_VISUALIZATION)
        public abstract Builder visualization(String visualization);

        @JsonProperty(FIELD_VISUALIZATION_CONFIG)
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = AggregationConfigDTO.FIELD_VISUALIZATION,
                visible = true)
        @Nullable
        public abstract Builder visualizationConfig(VisualizationConfigDTO visualizationConfig);

        @JsonProperty(FIELD_FORMATTING_SETTINGS)
        @Nullable
        public abstract Builder formattingSettings(WidgetFormattingSettings formattingSettings);

        @JsonProperty(FIELD_ROLLUP)
        public abstract Builder rollup(boolean roolup);

        public abstract AggregationConfigDTO build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_AggregationConfigDTO.Builder()
                    .rollup(true);
        }
    }
}
