package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.annotation.Nullable;

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

    interface Result {
        @JsonProperty("id")
        String id();
    }
}
