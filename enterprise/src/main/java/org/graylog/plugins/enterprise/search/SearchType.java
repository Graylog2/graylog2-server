package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * A search type represents parts of a query that generates a {@see Result result}.
 *
 * Plain queries only select a set of data but by themselves do not return any specific parts from it.
 * Typical search types are aggregations across fields, a list of messages and other metadata.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = SearchType.TYPE_FIELD,
        visible = true,
        defaultImpl = SearchType.Fallback.class)
@JsonAutoDetect
public interface SearchType {
    String TYPE_FIELD = "type";

    String type();

    @JsonProperty("id")
    String id();

    SearchType withId(String id);

    SearchType applyExecutionContext(ObjectMapper objectMapper, JsonNode state);

    @JsonAutoDetect
    class Fallback implements SearchType {

        @JsonProperty
        private String type;

        @JsonProperty
        private String id;

        @Override
        public String type() {
            return type;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public SearchType withId(String id) {
            this.id = id;
            return this;
        }

        @Override
        public SearchType applyExecutionContext(ObjectMapper objectMapper, JsonNode state) {
            return this;
        }

        @JsonAnySetter
        public void setType(String key, Object value) {
            // we ignore all the other values, we only want to be able to deserialize unknown search types
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
                    Objects.equals(id, fallback.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id);
        }
    }

    /**
     * Each search type should declare an implementation of its result conforming to this interface.
     *
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
}
