package org.graylog.plugins.enterprise.search.searchtypes.pivot;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
@JsonTypeName(PivotSort.Type)
public abstract class PivotSort implements SortSpec {
    public static final String Type = "pivot";

    @Override
    @JsonProperty(TYPE_FIELD)
    public abstract String type();

    @Override
    @JsonProperty(FIELD_FIELD)
    public abstract String field();

    @Override
    @JsonProperty(FIELD_DIRECTION)
    public abstract Direction direction();

    @JsonCreator
    public static PivotSort create(@JsonProperty(TYPE_FIELD) String type,
                                   @JsonProperty(FIELD_FIELD) String field,
                                   @JsonProperty(FIELD_DIRECTION) Direction direction) {
        return new AutoValue_PivotSort(type, field, direction);
    }
}
