package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * A search type represents parts of a query that generates a {@see Result result}.
 * <p>
 * Plain queries only select a set of data but by themselves do not return any specific parts from it.
 * Typical search types are aggregations across fields, a list of messages and other metadata.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = SearchType.TYPE_FIELD,
        visible = true,
        defaultImpl = SearchType.Fallback.class)
@JsonAutoDetect
public interface SearchType {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    @JsonProperty("id")
    String id();

    @Nullable
    @JsonProperty("filter")
    Filter filter();

    SearchType applyExecutionContext(ObjectMapper objectMapper, JsonNode state);

    /**
     * Each search type should declare an implementation of its result conforming to this interface.
     * <p>
     * The frontend components then make use of the structured data to display it.
     */
    interface Result {
        @JsonProperty("id")
        String id();

        /**
         * The json type info property of the surrounding SearchType class. Must be set manually by subclasses.
         */
        @JsonProperty("type")
        String type();
    }

    @JsonAutoDetect
    class Fallback implements SearchType {

        @JsonProperty
        private String type;

        @JsonProperty
        private String id;

        private Map<String, Object> props = Maps.newHashMap();

        @Nullable
        @JsonProperty
        private Filter filter;

        @Override
        public String type() {
            return type;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Filter filter() {
            return filter;
        }

        @Override
        public SearchType applyExecutionContext(ObjectMapper objectMapper, JsonNode state) {
            return this;
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
            Fallback fallback = (Fallback) o;
            return Objects.equals(type, fallback.type) &&
                    Objects.equals(id, fallback.id) &&
                    Objects.equals(props, fallback.props);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id, props);
        }
    }
}
