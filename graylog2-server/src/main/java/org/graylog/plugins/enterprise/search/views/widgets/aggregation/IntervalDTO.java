package org.graylog.plugins.enterprise.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = IntervalDTO.TYPE_FIELD,
        defaultImpl = TimeUnitIntervalDTO.class)
@JsonAutoDetect
public interface IntervalDTO {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();
}
