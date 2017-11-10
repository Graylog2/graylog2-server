package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collections;
import java.util.Set;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = ParameterBinding.TYPE_FIELD,
        visible = true,
        defaultImpl = ParameterBinding.Fallback.class)
@JsonAutoDetect
public interface ParameterBinding {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    Set<String> getReferences();

    class Fallback implements ParameterBinding {
        @JsonProperty
        private String type;

        @Override
        public String type() {
            return type;
        }

        @Override
        public Set<String> getReferences() {
            return Collections.emptySet();
        }

        @JsonAnySetter
        public void setType(String key, Object value) {
            // we ignore all the other values, we only want to be able to deserialize unknown parameter bindings
        }
    }
}
