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
package org.graylog.events.tags;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.database.BuildableMongoEntity;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = Tag.Builder.class)
public abstract class Tag implements BuildableMongoEntity<Tag, Tag.Builder> {
    public static final String FIELD_VALUE = "value";

    @ObjectId
    @Id
    @Nullable
    @Override
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_VALUE)
    public abstract String value();

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public abstract Builder toBuilder();

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder implements BuildableMongoEntity.Builder<Tag, Builder> {
        @Id
        @ObjectId
        @Override
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_VALUE)
        public abstract Builder value(String value);

        @Override
        public abstract Tag build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_Tag.Builder();
        }
    }
}
