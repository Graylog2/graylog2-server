package org.graylog2.datatier;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = DataTiersConfig.FIELD_TYPE, visible = true)
public interface DataTiersConfig {

    String FIELD_TYPE = "type";
    String FIELD_HOT_TIER = "hot_tier";

    @JsonProperty(FIELD_TYPE)
    String type();

}
