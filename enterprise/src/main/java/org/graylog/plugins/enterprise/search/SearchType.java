package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.annotation.Nullable;

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

    @JsonProperty(TYPE_FIELD)
    String type();

    @Nullable
    @JsonProperty("id")
    String id();

    SearchType withId(String id);

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

        @JsonAnySetter
        public void setType(String key, Object value) {
            // we ignore all the other values, we only want to be able to deserialize unknown search types
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
    }
}
