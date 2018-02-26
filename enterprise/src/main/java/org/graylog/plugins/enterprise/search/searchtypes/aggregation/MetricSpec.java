package org.graylog.plugins.enterprise.search.searchtypes.aggregation;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = MetricSpec.TYPE_FIELD,
        visible = true,
        defaultImpl = MetricSpec.Fallback.class)
public interface MetricSpec extends AggregationSpec {
    String TYPE_FIELD = "type";

    String type();

    @JsonAutoDetect
    class Fallback implements MetricSpec {
        @JsonProperty
        private String type;

        @Override
        public String type() {
            return type;
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
