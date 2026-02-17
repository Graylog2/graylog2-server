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

import jakarta.annotation.Nullable;
import org.bson.Document;

import java.util.Set;

/**
 * A raw marker entry from the fleet transaction log.
 *
 *  This is persisted in a non-entity collection, thus it has no Jackson mapping.
 *
 * @param seq       sequence number (the _id in MongoDB)
 * @param target    "fleet" or "collector"
 * @param targetIds fleet IDs or collector instance UIDs (always a set, even for single targets)
 * @param type      parsed marker type
 * @param rawType   original string from MongoDB (for logging unknown types)
 * @param payload   optional type-specific data (e.g., new_fleet_id for FLEET_REASSIGNED)
 */
public record TransactionMarker(long seq,
                                String target,
                                Set<String> targetIds,
                                MarkerType type,
                                String rawType,
                                @Nullable Document payload) {

    public static final String TARGET_FLEET = "fleet";
    public static final String TARGET_COLLECTOR = "collector";
}
