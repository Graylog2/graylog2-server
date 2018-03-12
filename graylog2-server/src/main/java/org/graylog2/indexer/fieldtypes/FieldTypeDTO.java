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
package org.graylog2.indexer.fieldtypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = FieldTypeDTO.Builder.class)
public abstract class FieldTypeDTO {
    static final String FIELD_NAME = "field_name";
    static final String FIELD_PHYSICAL_TYPE = "physical_type";

    @JsonProperty(FIELD_NAME)
    public abstract String fieldName();

    @JsonProperty(FIELD_PHYSICAL_TYPE)
    public abstract String physicalType();

    public static FieldTypeDTO create(String fieldName, String physicalType) {
        return builder().fieldName(fieldName).physicalType(physicalType).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_FieldTypeDTO.Builder();
        }

        @JsonProperty(FIELD_NAME)
        public abstract Builder fieldName(String fieldName);

        @JsonProperty(FIELD_PHYSICAL_TYPE)
        public abstract Builder physicalType(String physicalType);

        public abstract FieldTypeDTO build();
    }
}