package org.graylog.plugins.views.search.searchtypes.pivot;


import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;

/**
 * BucketSpecs describe configurations for aggregation buckets.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = BucketSpec.TYPE_FIELD,
        visible = true,
        defaultImpl = BucketSpec.Fallback.class)
public interface BucketSpec extends PivotSpec {
    String TYPE_FIELD = "type";

    @JsonProperty
    String type();

    @JsonAutoDetect
    class Fallback implements BucketSpec {
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
            Fallback fallback = (Fallback) o;
            return Objects.equals(type, fallback.type) &&
                    Objects.equals(props, fallback.props);
        }

        @Override
        public int hashCode() {

            return Objects.hash(type, props);
        }
    }
}
