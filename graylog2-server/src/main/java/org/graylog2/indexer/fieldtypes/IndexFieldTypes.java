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
import com.google.common.collect.ImmutableMap;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
@JsonDeserialize(builder = IndexFieldTypes.Builder.class)
public abstract class IndexFieldTypes {
    private static final String FIELD_ID = "id";
    static final String FIELD_INDEX_SET_ID = "index_set_id";
    static final String FIELD_INDEX_NAME = "index_name";
    private static final String FIELD_FIELDS = "fields";

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
    public abstract ImmutableMap<String, FieldType> fields();

    public static IndexFieldTypes create(String indexSetId, String indexName, Map<String, FieldType> fields) {
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
            return new AutoValue_IndexFieldTypes.Builder();
        }

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_INDEX_SET_ID)
        public abstract Builder indexSetId(String indexSetId);

        @JsonProperty(FIELD_INDEX_NAME)
        public abstract Builder indexName(String indexName);

        abstract ImmutableMap.Builder<String, FieldType> fieldsBuilder();

        @JsonProperty(FIELD_FIELDS)
        public Builder fields(Map<String, FieldType> fields) {
            fieldsBuilder().putAll(fields);
            return this;
        }

        public abstract IndexFieldTypes build();
    }
}