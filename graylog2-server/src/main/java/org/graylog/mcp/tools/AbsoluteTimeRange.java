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
package org.graylog.mcp.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;

public record AbsoluteTimeRange(
        @JsonProperty("from")
        @JsonPropertyDescription("Start of the time range in ISO 8601 format (e.g. 2024-01-01T00:00:00.000Z)")
        @NotBlank
        String from,

        @JsonProperty("to")
        @JsonPropertyDescription("End of the time range in ISO 8601 format (e.g. 2024-01-01T01:00:00.000Z)")
        @NotBlank
        String to
) {}
