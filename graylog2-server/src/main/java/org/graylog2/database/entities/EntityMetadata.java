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
import org.joda.time.DateTime;

import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = EntityMetadata.Builder.class)
public abstract class EntityMetadata {

    public static final String SCOPES = "scopes";
    public static final String REVISION = "revision";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";

    @JsonProperty(SCOPES)
    public abstract Set<String> scopes();

    @JsonProperty(REVISION)
    public abstract int revision();

    @JsonProperty(CREATED_AT)
    public abstract DateTime createdAt();

    @JsonProperty(UPDATED_AT)
    public abstract DateTime updatedAt();

    public static Builder builder() {
        return Builder.create().revision(0);
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_EntityMetadata.Builder();
        }

        @JsonProperty(SCOPES)
        public abstract Builder scopes(Set<String> scopes);

        @JsonProperty(REVISION)
        public abstract Builder revision(int revision);

        @JsonProperty(CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

        @JsonProperty(UPDATED_AT)
        public abstract Builder updatedAt(DateTime updatedAt);

        public abstract EntityMetadata build();
    }
}
