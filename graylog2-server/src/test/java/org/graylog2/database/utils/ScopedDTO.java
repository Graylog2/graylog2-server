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
package org.graylog2.database.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.database.entities.ScopedEntity;
import org.mongojack.Id;
import org.mongojack.ObjectId;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = ScopedDTO.Builder.class)
public abstract class ScopedDTO extends ScopedEntity {

    @JsonProperty("name")
    public abstract String name();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder extends AbstractBuilder<Builder> {
        @Override
        @Id
        @ObjectId
        @JsonProperty("id")
        public abstract Builder id(String id);

        @Override
        @JsonProperty("_scope")
        public abstract Builder scope(String scope);

        @JsonProperty("name")
        public abstract Builder name(String name);

        @JsonCreator
        public static ScopedDTO.Builder create() {
            return new AutoValue_ScopedDTO.Builder();
        }

        public abstract ScopedDTO build();
    }

}
