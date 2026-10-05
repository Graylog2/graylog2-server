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
package org.graylog2.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Lightweight response DTO for the paginated notification entity table.
 * Constructed from a raw MongoDB document plus rendered title/description.
 */
public record NotificationSummaryDto(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("key") @Nullable String key,
        @JsonProperty("severity") String severity,
        @JsonProperty("node_id") String nodeId,
        @JsonProperty("title") @Nullable String title,
        @JsonProperty("description") @Nullable String description,
        @JsonProperty("details") @Nullable Map<String, Object> details,
        @JsonProperty("timestamp") String timestamp
) {}
