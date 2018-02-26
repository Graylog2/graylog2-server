package org.graylog.plugins.enterprise.search.searchtypes.aggregation;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Iterables;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = GroupSpec.TYPE_FIELD,
        visible = true,
        defaultImpl = GroupSpec.Fallback.class)
public interface GroupSpec extends AggregationSpec {
    String TYPE_FIELD = "type";

    String type();

    @JsonProperty
    List<MetricSpec> metrics();

    @JsonProperty
    List<GroupSpec> groups();

    @Override
    default Iterable<AggregationSpec> subAggregations() {
        return Iterables.concat(metrics(), groups());
    }

    @JsonAutoDetect
    class Fallback implements GroupSpec {
        @JsonProperty
        private String type;

        @Override
        public String type() {
            return type;
        }

        @Override
        public List<MetricSpec> metrics() {
            return Collections.emptyList();
        }

        @Override
        public List<GroupSpec> groups() {
            return Collections.emptyList();
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
