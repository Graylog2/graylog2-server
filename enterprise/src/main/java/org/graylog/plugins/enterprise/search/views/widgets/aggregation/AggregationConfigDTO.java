package org.graylog.plugins.enterprise.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.views.WidgetConfigDTO;

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


    @JsonProperty(FIELD_ROW_PIVOTS)
    public abstract List<String> rowPivots();

    @JsonProperty(FIELD_COLUMN_PIVOTS)
    public abstract List<String> columnPivots();

    @JsonProperty(FIELD_SERIES)
    public abstract List<String> series();

    @JsonProperty(FIELD_SORT)
    public abstract List<String> sort();

    @JsonProperty(FIELD_VISUALIZATION)
    public abstract String visualization();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_ROW_PIVOTS)
        public abstract Builder rowPivots(List<String> rowPivots);

        @JsonProperty(FIELD_COLUMN_PIVOTS)
        public abstract Builder columnPivots(List<String> columnPivots);

        @JsonProperty(FIELD_SERIES)
        public abstract Builder series(List<String> series);

        @JsonProperty(FIELD_SORT)
        public abstract Builder sort(List<String> sort);

        @JsonProperty(FIELD_VISUALIZATION)
        public abstract Builder visualization(String visualization);

        public abstract AggregationConfigDTO build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_AggregationConfigDTO.Builder();
        }
    }
}
