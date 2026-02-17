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
import org.graylog2.database.BuildableMongoEntity;

@AutoValue
@JsonDeserialize(builder = SourceDTO.Builder.class)
public abstract class SourceDTO implements BuildableMongoEntity<SourceDTO, SourceDTO.Builder> {
    public static final String FIELD_FLEET_ID = "fleet_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_ENABLED = "enabled";
    public static final String FIELD_CONFIG = "config";

    @JsonProperty(FIELD_FLEET_ID)
    public abstract String fleetId();

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonProperty(FIELD_ENABLED)
    public abstract boolean enabled();

    @JsonProperty(FIELD_CONFIG)
    public abstract SourceConfig config();

    public static Builder builder() {
        return AutoValue_SourceDTO.Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder implements BuildableMongoEntity.Builder<SourceDTO, SourceDTO.Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_SourceDTO.Builder()
                    .enabled(true);
        }

        @JsonProperty(FIELD_FLEET_ID)
        public abstract Builder fleetId(String fleetId);

        @JsonProperty(FIELD_NAME)
        public abstract Builder name(String name);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_ENABLED)
        public abstract Builder enabled(boolean enabled);

        @JsonProperty(FIELD_CONFIG)
        public abstract Builder config(SourceConfig config);

        public abstract SourceDTO build();
    }
}
