package org.graylog.plugins.enterprise.search.views.widgets.aggregation.sort;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
@JsonTypeName(PivotSortConfig.Type)
public abstract class PivotSortConfig implements SortConfigDTO {
    public static final String Type = "pivot";

    @Override
    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @Override
    @JsonProperty(FIELD_FIELD)
    public abstract String field();

    @Override
    @JsonProperty(FIELD_DIRECTION)
    public abstract Direction direction();

    @JsonCreator
    public static PivotSortConfig create(@JsonProperty(FIELD_FIELD) String field,
                                         @JsonProperty(FIELD_DIRECTION) Direction direction) {
        return new AutoValue_PivotSortConfig(PivotSortConfig.Type, field, direction);
    }
}
