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
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = FieldTypes.Builder.class)
public abstract class FieldTypes {
    private static final String FIELD_FIELD_NAME = "field_name";
    private static final String FIELD_TYPES = "types";

    @JsonProperty(FIELD_FIELD_NAME)
    public abstract String fieldName();

    @JsonProperty(FIELD_TYPES)
    public abstract ImmutableSet<Type> types();

    public static Builder builder() {
        return Builder.create();
    }

    public static FieldTypes create(String fieldName, Set<Type> types) {
        return builder()
                .fieldName(fieldName)
                .types(types)
                .build();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_FieldTypes.Builder();
        }

        @JsonProperty(FIELD_FIELD_NAME)
        public abstract Builder fieldName(String name);

        abstract ImmutableSet.Builder<Type> typesBuilder();

        @JsonProperty(FIELD_TYPES)
        public Builder types(Set<Type> types) {
            typesBuilder().addAll(types);
            return this;
        }

        public Builder addType(Type type) {
            typesBuilder().add(type);
            return this;
        }

        public abstract FieldTypes build();
    }

    @AutoValue
    @JsonDeserialize(builder = FieldTypes.Type.Builder.class)
    public static abstract class Type {
        private static final String FIELD_TYPE = "type";
        private static final String FIELD_PROPERTIES = "properties";
        private static final String FIELD_INDEX_NAMES = "index_names";

        @JsonProperty(FIELD_TYPE)
        public abstract String type();

        @JsonProperty(FIELD_PROPERTIES)
        public abstract ImmutableSet<String> properties();

        @JsonProperty(FIELD_INDEX_NAMES)
        public abstract ImmutableSet<String> indexNames();

        public static Type.Builder builder() {
            return Type.Builder.create();
        }

        public static Type createType(String type, Set<String> properties) {
            return Type.builder()
                    .type(type)
                    .properties(properties)
                    .indexNames(Collections.emptySet())
                    .build();
        }

        public abstract Type.Builder toBuilder();

        public Type withIndexNames(Set<String> indexNames) {
            return toBuilder().indexNames(indexNames).build();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            @JsonCreator
            public static Type.Builder create() {
                return new AutoValue_FieldTypes_Type.Builder();
            }

            @JsonProperty(FIELD_TYPE)
            public abstract Type.Builder type(String type);

            abstract ImmutableSet.Builder<String> propertiesBuilder();

            @JsonProperty(FIELD_PROPERTIES)
            public Builder properties(Set<String> properties) {
                propertiesBuilder().addAll(properties);
                return this;
            }

            abstract ImmutableSet.Builder<String> indexNamesBuilder();

            @JsonProperty(FIELD_INDEX_NAMES)
            public Builder indexNames(Set<String> indexNames) {
                indexNamesBuilder().addAll(indexNames);
                return this;
            }

            public abstract Type build();
        }
    }
}