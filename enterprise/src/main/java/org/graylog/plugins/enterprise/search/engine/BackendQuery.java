package org.graylog.plugins.enterprise.search.engine;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = BackendQuery.TYPE_FIELD,
        visible = true,
        defaultImpl = BackendQuery.Fallback.class)
public interface BackendQuery {
    String TYPE_FIELD = "type";

    String type();

    @JsonAutoDetect
    class Fallback implements BackendQuery {
        @JsonProperty
        private String type;

        @Override
        public String type() {
            return type;
        }

        @JsonAnySetter
        public void setType(String key, Object value) {
            // we ignore all the other values, we only want to be able to deserialize unknown objects
        }
    }
}
