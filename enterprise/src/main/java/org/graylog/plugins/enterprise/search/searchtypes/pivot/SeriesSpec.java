package org.graylog.plugins.enterprise.search.searchtypes.pivot;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.annotation.Nullable;
import java.util.Objects;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = SeriesSpec.TYPE_FIELD,
        visible = true,
        defaultImpl = SeriesSpec.Fallback.class)
public interface SeriesSpec extends PivotSpec {
    String TYPE_FIELD = "type";

    String type();

    @JsonProperty
    @Nullable
    String id();

    @JsonAutoDetect
    class Fallback implements SeriesSpec {
        @JsonProperty
        private String type;

        @JsonProperty
        private String id;

        @Override
        public String type() {
            return type;
        }

        @Nullable
        @Override
        public String id() {
            return id;
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
            return Objects.equals(type, fallback.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }
    }
}
