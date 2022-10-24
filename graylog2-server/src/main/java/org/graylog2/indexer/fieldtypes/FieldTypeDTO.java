/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.indexer.fieldtypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonDeserialize(builder = FieldTypeDTO.Builder.class)
public abstract class FieldTypeDTO {
    static final String FIELD_NAME = "field_name";
    static final String FIELD_PHYSICAL_TYPE = "physical_type";
    static final String FIELD_PROPERTIES = "properties";
    static final String FIELD_STREAMS = "streams";

    public enum Properties {
        FIELDDATA
    }

    @JsonProperty(FIELD_NAME)
    public abstract String fieldName();

    @JsonProperty(FIELD_PHYSICAL_TYPE)
    public abstract String physicalType();

    @JsonProperty(FIELD_PROPERTIES)
    public abstract Set<Properties> properties();

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    public static FieldTypeDTO create(String fieldName, String physicalType) {
        return builder().fieldName(fieldName).physicalType(physicalType).build();
    }

    public static FieldTypeDTO create(String fieldName, String physicalType, Set<Properties> properties) {
        return builder()
                .fieldName(fieldName)
                .physicalType(physicalType)
                .properties(properties)
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_FieldTypeDTO.Builder()
                    .properties(Set.of())
                    .streams(Set.of());
        }

        @JsonProperty(FIELD_NAME)
        public abstract Builder fieldName(String fieldName);

        @JsonProperty(FIELD_PHYSICAL_TYPE)
        public abstract Builder physicalType(String physicalType);

        @JsonProperty(FIELD_PROPERTIES)
        public abstract Builder properties(Set<Properties> properties);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        public abstract FieldTypeDTO build();
    }
}
