package org.graylog.plugins.enterprise.search.views.widgets.aggregation.sort;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
@JsonTypeName(SeriesSortConfig.Type)
public abstract class SeriesSortConfig implements SortConfigDTO {
    public static final String Type = "series";

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
    public static SeriesSortConfig create(@JsonProperty(FIELD_FIELD) String field,
                                          @JsonProperty(FIELD_DIRECTION) Direction direction) {
        return new AutoValue_SeriesSortConfig(SeriesSortConfig.Type, field, direction);
    }
}
