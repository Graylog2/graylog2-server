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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = TestScopedEntity.Builder.class)
public abstract class TestScopedEntity extends ScopedEntity {

    public static final String TITLE = "title";

    // Sample arbitrary entity field.
    @JsonProperty(TITLE)
    public abstract String title();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder extends AbstractBuilder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_TestScopedEntity.Builder()
                    .scope(DefaultEntityScope.NAME);
        }

        @JsonProperty(TITLE)
        public abstract Builder title(String title);

        public abstract TestScopedEntity build();
    }
}
