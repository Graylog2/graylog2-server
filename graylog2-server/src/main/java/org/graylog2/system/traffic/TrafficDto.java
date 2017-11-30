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
package org.graylog2.system.traffic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.util.Collections;
import java.util.Map;

@AutoValue
@JsonDeserialize(builder = AutoValue_TrafficDto.Builder.class)
public abstract class TrafficDto {

    @Id
    @ObjectId
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract DateTime bucket();

    @JsonProperty
    public abstract Map<String, Long> input();

    @JsonProperty
    public abstract Map<String, Long> output();

    @JsonProperty
    public abstract Map<String, Long> decoded();

    public static Builder builder() {
        return new AutoValue_TrafficDto.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public Builder() {
            decoded(Collections.emptyMap());
        }

        @Id
        @ObjectId
        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder bucket(DateTime bucket);

        @JsonProperty
        public abstract Builder input(Map<String, Long> inputTraffic);

        @JsonProperty
        public abstract Builder output(Map<String, Long> outputTraffic);

        @JsonProperty
        public abstract Builder decoded(Map<String, Long> decodedTraffic);

        public abstract TrafficDto build();
    }
}
