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

/**
 * Types of markers in the fleet transaction log. Stored as strings in MongoDB.
 * Unknown values (from newer server versions) are parsed as {@link #UNKNOWN} to ensure
 * forward compatibility across cluster upgrades.
 *
 * This is persisted in a non-entity collection, thus it has no Jackson mapping.
 */
public enum MarkerType {
    CONFIG_CHANGED,
    RESTART,
    DISCOVERY_RUN,
    FLEET_REASSIGNED,
    UNKNOWN;

    /**
     * Parse a marker type from its string representation, returning {@link #UNKNOWN}
     * for unrecognized values instead of throwing.
     */
    public static MarkerType fromString(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
