package org.graylog2.inputs.persistence;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = InputStateData.TYPE_FIELD,
        visible = true,
        defaultImpl = InputStateData.Fallback.class
)
public interface InputStateData {
    String TYPE_FIELD = "type";

    @JsonProperty
    String type();

    public interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }

    @JsonAutoDetect
    class Fallback implements InputStateData {
        @JsonProperty
        private String type;

        private Map<String, Object> props = Maps.newHashMap();

        @Override
        public String type() {
            return type;
        }

        @JsonAnySetter
        public void setProperties(String key, Object value) {
            props.put(key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getProperties() {
            return props;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            InputStateData.Fallback fallback = (InputStateData.Fallback) o;
            return Objects.equals(type, fallback.type) &&
                    Objects.equals(props, fallback.props);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, props);
        }
    }
}
