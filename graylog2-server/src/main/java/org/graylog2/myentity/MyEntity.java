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
package org.graylog2.myentity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.database.entities.Entity;
import org.graylog2.database.entities.EntityMetadata;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * This example entity implements the Entity interface, since AutoValue classes cannot extend them.
 * Again, not as good, but it's something.
 */
@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = MyEntity.Builder.class)
public abstract class MyEntity implements Entity {

    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";

    @Override
    @Id
    @ObjectId
    @Nullable
    @JsonProperty(ID)
    public abstract String id();

    @Override
    @Nullable
    @JsonProperty(METADATA)
    public abstract EntityMetadata metadata();

    // Sample arbitrary entity fields.
    @JsonProperty(TITLE)
    public abstract String title();

    @JsonProperty(DESCRIPTION)
    public abstract Optional<String> description();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            // TODO (optional): Supply empty metadata default for existing objects.
            return new AutoValue_MyEntity.Builder();
        }

        @Id
        @ObjectId
        @JsonProperty(ID)
        public abstract Builder id(String id);

        @JsonProperty(METADATA)
        public abstract Builder metadata(EntityMetadata metadata);

        @JsonProperty(TITLE)
        public abstract Builder title(String title);

        @JsonProperty(DESCRIPTION)
        public abstract Builder description(Optional<String> description);

        public abstract MyEntity build();
    }
}
