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
package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = FieldTypesForStreamsRequest.Builder.class)
public abstract class FieldTypesForStreamsRequest {
    private static final String FIELD_STREAMS = "streams";

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        public abstract FieldTypesForStreamsRequest build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_FieldTypesForStreamsRequest.Builder();
        }
    }
}
