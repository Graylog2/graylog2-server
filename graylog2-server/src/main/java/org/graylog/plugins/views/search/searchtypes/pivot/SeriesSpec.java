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
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = SeriesSpec.TYPE_FIELD,
        visible = true,
        defaultImpl = SeriesSpec.Fallback.class)
public interface SeriesSpec extends PivotSpec {
    String TYPE_FIELD = "type";

    @JsonProperty
    String type();

    @JsonProperty
    @Nullable
    String id();

    @JsonProperty
    String field();

    default String literal() {
        return type() + "(" + Strings.nullToEmpty(field()) + ")";
    }

    @JsonAutoDetect
    class Fallback implements SeriesSpec {
        @JsonProperty
        private String type;

        @JsonProperty
        private String id;

        @JsonProperty
        private String field;

        private Map<String, Object> props = Maps.newHashMap();

        @Override
        public String type() {
            return type;
        }

        @Nullable
        @Override
        public String id() {
            return id;
        }

        @Override
        public String field() { return field; }

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
