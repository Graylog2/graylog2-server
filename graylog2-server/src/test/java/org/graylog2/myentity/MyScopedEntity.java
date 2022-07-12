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
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.ScopedEntity;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * This example entity implements the Entity interface, since AutoValue classes cannot extend them.
 * Again, not as good, but it's something.
 */
@JsonAutoDetect
@JsonDeserialize(builder = MyScopedEntity.Builder.class)
public abstract class MyScopedEntity extends ScopedEntity {

    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";

    // Sample arbitrary entity fields.
    @JsonProperty(TITLE)
    public abstract String title();

    @JsonProperty(DESCRIPTION)
    public abstract Optional<String> description();

    // TODO: try to figure out a better way to call the parent builder (ScopedEntity::Builder) to supply the default scope.
    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder extends ScopedEntity.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_MyScopedEntity.Builder()
                    .scope(DefaultEntityScope.NAME);
        }

        @JsonProperty(TITLE)
        public abstract Builder title(String title);

        @JsonProperty(DESCRIPTION)
        public abstract Builder description(@Nullable String description);

        public abstract MyScopedEntity build();
    }
}
