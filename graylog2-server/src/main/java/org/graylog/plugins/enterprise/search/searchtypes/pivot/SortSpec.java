package org.graylog.plugins.enterprise.search.searchtypes.pivot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = SortSpec.TYPE_FIELD,
        visible = true)
public interface SortSpec {
    enum Direction {
        Ascending,
        Descending
    }

    String TYPE_FIELD = "type";
    String FIELD_FIELD = "field";
    String FIELD_DIRECTION = "direction";

    @JsonProperty(TYPE_FIELD)
    String type();
    @JsonProperty(FIELD_FIELD)
    String field();
    @JsonProperty(FIELD_DIRECTION)
    Direction direction();
}
