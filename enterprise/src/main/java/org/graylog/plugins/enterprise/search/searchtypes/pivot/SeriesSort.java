package org.graylog.plugins.enterprise.search.searchtypes.pivot;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
@JsonTypeName(SeriesSort.Type)
public abstract class SeriesSort implements SortSpec {
    public static final String Type = "series";

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
    public static SeriesSort create(@JsonProperty(TYPE_FIELD) String type,
                                    @JsonProperty(FIELD_FIELD) String field,
                                    @JsonProperty(FIELD_DIRECTION) Direction direction) {
        return new AutoValue_SeriesSort(type, field, direction);
    }
}
