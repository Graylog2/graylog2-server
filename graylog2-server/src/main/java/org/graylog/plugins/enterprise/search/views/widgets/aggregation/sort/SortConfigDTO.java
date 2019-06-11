package org.graylog.plugins.enterprise.search.views.widgets.aggregation.sort;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = SortConfigDTO.FIELD_TYPE
)
@JsonAutoDetect
public interface SortConfigDTO {
    String FIELD_TYPE = "type";
    String FIELD_FIELD = "field";
    String FIELD_DIRECTION = "direction";

    enum Direction {
        Ascending,
        Descending
    }

    @JsonProperty(FIELD_TYPE)
    String type();

    @JsonProperty(FIELD_FIELD)
    String field();

    @JsonProperty(FIELD_DIRECTION)
    Direction direction();
}
