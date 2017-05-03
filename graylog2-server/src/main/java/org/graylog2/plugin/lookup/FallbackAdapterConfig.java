package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is the dummy config that accepts anything and has a marker method to detect a missing plugin.
 * Otherwise loading the config from the database fails hard.
 */
@JsonAutoDetect
public class FallbackAdapterConfig implements LookupDataAdapterConfiguration {

    @JsonProperty
    private String type;

    @Override
    public String type() {
        return type;
    }

    @JsonAnySetter
    public void setType(String key, Object value) {
        // we ignore all the other values, we only want to be able to deserialize unknown configs
    }
}
