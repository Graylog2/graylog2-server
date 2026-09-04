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
package org.graylog.collectors.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.graylog.collectors.db.SourceConfig;
import org.graylog.collectors.db.SourceDTO;

public record SourceResponse(
        @JsonProperty("id") String id,
        @JsonProperty("fleet_id") String fleetId,
        @JsonProperty("name") String name,
        @Nullable @JsonProperty("description") String description,
        @JsonProperty("enabled") boolean enabled,
        @JsonProperty("type") String type,
        @JsonProperty("config") SourceConfig config) {

    public static SourceResponse fromDTO(SourceDTO dto) {
        return new SourceResponse(dto.id(), dto.fleetId(), dto.name(),
                dto.description(), dto.enabled(), dto.config().type(), dto.config());
    }
}
