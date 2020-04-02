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
package org.graylog2.plugin.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonDeserialize(builder = MissingStreamPermissionError.Builder.class)
public abstract class MissingStreamPermissionError {

    private static final String FIELD_ERROR_MESSAGE = "message";
    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_TYPE = "type";

    @JsonProperty(FIELD_ERROR_MESSAGE)
    public abstract String errorMessage();

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_MissingStreamPermissionError.Builder()
                    .type("missing stream permission error");
        }

        @JsonProperty(FIELD_ERROR_MESSAGE)
        public abstract Builder errorMessage(String errorMessage);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        public abstract MissingStreamPermissionError build();
    }
}
