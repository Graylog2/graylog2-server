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
package org.graylog2.database.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.graylog2.database.MongoEntity;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = EntitySource.Builder.class)
public abstract class EntitySource implements MongoEntity {
    public static final String USER_DEFINED = "USER_DEFINED";
    public static final String CONTENT_PACK = "CONTENT_PACK";

    public static final String FIELD_ID = "id";
    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_ENTITY_ID = "entity_id";
    public static final String FIELD_PARENT_ID = "parent_id";

    @Nullable
    @ObjectId
    @JsonProperty(value = FIELD_ENTITY_ID, access = JsonProperty.Access.WRITE_ONLY)
    public abstract String entityId();

    @JsonProperty(FIELD_SOURCE)
    public abstract String source();

    @JsonProperty(FIELD_PARENT_ID)
    public abstract Optional<String> parentId();

    public boolean isCloned() {
        return parentId().isPresent();
    }

    public static EntitySource defaultSource() {
        return Builder.create().build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EntitySource.Builder()
                    .source(USER_DEFINED);
        }

        @ObjectId
        @Id
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @ObjectId
        @JsonProperty(FIELD_ENTITY_ID)
        public abstract Builder entityId(String entityId);

        @JsonProperty(FIELD_SOURCE)
        public abstract Builder source(String source);

        @JsonProperty(FIELD_PARENT_ID)
        public abstract Builder parentId(String parentId);

        public abstract EntitySource build();
    }
}
