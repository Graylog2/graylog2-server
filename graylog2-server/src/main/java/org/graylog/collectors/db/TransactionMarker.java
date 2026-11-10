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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.mongojack.Id;

import java.time.Instant;
import java.util.Set;

/**
 * A marker entry from the fleet transaction log.
 *
 * @param seq           sequence number (the _id in MongoDB)
 * @param target        "fleet" or "collector"
 * @param targetIds     fleet IDs or collector instance UIDs (always a set, even for single targets)
 * @param type          parsed marker type
 * @param payload       optional type-specific data (e.g., new_fleet_id for FLEET_REASSIGNED)
 * @param createdAt     timestamp when the marker was created
 * @param createdBy     node ID of the server that created the marker
 * @param createdByUser username of the actor, or null for system-initiated actions
 */
public record TransactionMarker(
        @Id @JsonProperty(FIELD_ID) long seq,
        @JsonProperty(FIELD_TARGET) String target,
        @JsonProperty(FIELD_TARGET_ID) Set<String> targetIds,
        @JsonProperty(FIELD_TYPE) MarkerType type,
        @JsonProperty(FIELD_PAYLOAD) @Nullable MarkerPayload payload,
        @JsonProperty(FIELD_CREATED_AT) @Nullable Instant createdAt,
        @JsonProperty(FIELD_CREATED_BY) @Nullable String createdBy,
        @JsonProperty(FIELD_CREATED_BY_USER) @Nullable String createdByUser) {

    public static final String FIELD_ID = "_id";
    public static final String FIELD_TARGET = "target";
    public static final String FIELD_TARGET_ID = "target_id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_PAYLOAD = "payload";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_CREATED_BY = "created_by";
    public static final String FIELD_CREATED_BY_USER = "created_by_user";

    public static final String TARGET_FLEET = "fleet";
    public static final String TARGET_COLLECTOR = "collector";
}
