package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = LookupCacheConfiguration.TYPE_FIELD, visible = true)
public interface LookupCacheConfiguration {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

}
