package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonTypeName(DataTableVisualizationConfigDTO.NAME)
@JsonDeserialize(builder = DataTableVisualizationConfigDTO.Builder.class)
public abstract class DataTableVisualizationConfigDTO implements VisualizationConfigDTO {
    public static final String NAME = "table";

    private static final String FIELD_PINNED_COLUMNS = "pinnedColumns";

    @JsonProperty(FIELD_PINNED_COLUMNS)
    public abstract List<String> pinnedColumns();

    public static DataTableVisualizationConfigDTO.Builder builder() {
        return Builder.create();
    }

    public abstract DataTableVisualizationConfigDTO.Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        @JsonCreator
        public static DataTableVisualizationConfigDTO.Builder create() {
            return new AutoValue_DataTableVisualizationConfigDTO.Builder()
                    .pinnedColumns(List.of());
        }

        @JsonProperty(FIELD_PINNED_COLUMNS)
        public abstract DataTableVisualizationConfigDTO.Builder pinnedColumns(List<String> pinnedColumns);

        public abstract DataTableVisualizationConfigDTO build();
    }
}
