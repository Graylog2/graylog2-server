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
package org.graylog.collectors.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.graylog2.database.BuildableMongoEntity;

import java.time.Instant;

@AutoValue
@JsonDeserialize(builder = FleetDTO.Builder.class)
public abstract class FleetDTO implements BuildableMongoEntity<FleetDTO, FleetDTO.Builder> {
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_TARGET_VERSION = "target_version";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_UPDATED_AT = "updated_at";

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @Nullable
    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @Nullable
    @JsonProperty(FIELD_TARGET_VERSION)
    public abstract String targetVersion();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract Instant createdAt();

    @JsonProperty(FIELD_UPDATED_AT)
    public abstract Instant updatedAt();

    public static Builder builder() {
        return AutoValue_FleetDTO.Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder implements BuildableMongoEntity.Builder<FleetDTO, FleetDTO.Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_FleetDTO.Builder();
        }

        @JsonProperty(FIELD_NAME)
        public abstract Builder name(String name);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(@Nullable String description);

        @JsonProperty(FIELD_TARGET_VERSION)
        public abstract Builder targetVersion(@Nullable String targetVersion);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(Instant createdAt);

        @JsonProperty(FIELD_UPDATED_AT)
        public abstract Builder updatedAt(Instant updatedAt);

        public abstract FleetDTO build();
    }
}
