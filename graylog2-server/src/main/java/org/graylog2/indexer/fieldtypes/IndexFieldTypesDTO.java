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
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = IndexFieldTypesDTO.Builder.class)
public abstract class IndexFieldTypesDTO {
    private static final String FIELD_ID = "id";
    static final String FIELD_INDEX_SET_ID = "index_set_id";
    static final String FIELD_INDEX_NAME = "index_name";
    static final String FIELD_FIELDS = "fields";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_INDEX_SET_ID)
    public abstract String indexSetId();

    @JsonProperty(FIELD_INDEX_NAME)
    public abstract String indexName();

    @JsonProperty(FIELD_FIELDS)
    public abstract ImmutableSet<FieldTypeDTO> fields();

    public static IndexFieldTypesDTO create(String indexSetId, String indexName, Set<FieldTypeDTO> fields) {
        return builder()
                .indexSetId(indexSetId)
                .indexName(indexName)
                .fields(fields)
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
            return new AutoValue_IndexFieldTypesDTO.Builder();
        }

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_INDEX_SET_ID)
        public abstract Builder indexSetId(String indexSetId);

        @JsonProperty(FIELD_INDEX_NAME)
        public abstract Builder indexName(String indexName);

        abstract ImmutableSet.Builder<FieldTypeDTO> fieldsBuilder();

        @JsonProperty(FIELD_FIELDS)
        public Builder fields(Set<FieldTypeDTO> fields) {
            fieldsBuilder().addAll(fields);
            return this;
        }

        public abstract IndexFieldTypesDTO build();
    }
}