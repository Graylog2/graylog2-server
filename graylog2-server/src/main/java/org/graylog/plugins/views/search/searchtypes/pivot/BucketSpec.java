/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
