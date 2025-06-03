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
package org.graylog2.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@AutoValue
@JsonDeserialize(builder = MongoEntityMetadata.Builder.class)
public abstract class MongoEntityMetadata {
    public static final String EMPTY_NAMESPACE = "empty";

    public static final MongoEntityMetadata EMPTY = builder()
            .namespace(EMPTY_NAMESPACE)
            .createdAt(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))
            .updatedAt(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))
            .build();

    @JsonProperty("namespace")
    public abstract String namespace();

    @JsonProperty("created_at")
    public abstract ZonedDateTime createdAt();

    @JsonProperty("updated_at")
    public abstract ZonedDateTime updatedAt();

    public static MongoEntityMetadata create(String namespace, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        return builder()
                .namespace(namespace)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_MongoEntityMetadata.Builder();
        }

        @JsonProperty("namespace")
        public abstract Builder namespace(String namespace);

        @JsonProperty("created_at")
        public abstract Builder createdAt(ZonedDateTime createdAt);

        @JsonProperty("updated_at")
        public abstract Builder updatedAt(ZonedDateTime updatedAt);

        public abstract MongoEntityMetadata build();
    }
}
