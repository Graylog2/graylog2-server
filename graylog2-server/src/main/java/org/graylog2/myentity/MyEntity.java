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

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * This example entity implements the Entity interface, since AutoValue classes cannot extend them.
 * Again, not as good, but it's something.
 */
@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = MyEntity.Builder.class)
public abstract class MyEntity extends Entity<MyEntity> {

    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";

    // Sample arbitrary entity fields.
    @JsonProperty(TITLE)
    public abstract String title();

    @JsonProperty(DESCRIPTION)
    public abstract Optional<String> description();

    @Override
    public MyEntity withMetadata(EntityMetadata metadata) {
        return toBuilder().metadata(metadata).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder extends Entity.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_MyEntity.Builder()
                    // This needs to be done manually for each entity, I think there is no way to do it automatically
                    // One option could be to write an auto-value extension
                    .metadata(EntityMetadata.createDefault());
        }

        @JsonProperty(TITLE)
        public abstract Builder title(String title);

        @JsonProperty(DESCRIPTION)
        public abstract Builder description(@Nullable String description);

        public abstract MyEntity build();
    }
}
